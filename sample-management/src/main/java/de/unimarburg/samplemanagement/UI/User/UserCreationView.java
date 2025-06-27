package de.unimarburg.samplemanagement.UI.User;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.User;
import de.unimarburg.samplemanagement.repository.UserRepository;
import de.unimarburg.samplemanagement.security.Roles;
import de.unimarburg.samplemanagement.utils.GENERAL_UTIL;
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

@Route("/add_user")
public class UserCreationView extends HorizontalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final TextField usernameField = new TextField("Username");
    private final EmailField emailField = new EmailField("Email");
    private final PasswordField passwordField = new PasswordField("Password");
    private final RadioButtonGroup<String> roleSelector = new RadioButtonGroup<>();

    private final Button createButton = new Button("Create User");

    private final Grid<User> userGrid = new Grid<>(User.class);

    public UserCreationView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        add(SIDEBAR_FACTORY.getSidebar(null));
        VerticalLayout content = new VerticalLayout();
        content.setWidth("70%");

        H2 title = new H2("Create New User");

        roleSelector.setLabel("Select Role");
        roleSelector.setItems(Roles.USER, Roles.ADMIN);
        roleSelector.setValue(Roles.USER); // default

        FormLayout form = new FormLayout();
        passwordField.setRevealButtonVisible(true); // 👁 Show/hide toggle
        passwordField.setWidthFull();  // Make password field take full form width

        Button generateCreatePasswordButton = new Button("Generate Password", e -> {
            String generatedPassword = GENERAL_UTIL.generateRandomPassword(8);
            passwordField.setValue(generatedPassword);
        });

        HorizontalLayout buttonWrapper = new HorizontalLayout(generateCreatePasswordButton);
        buttonWrapper.setWidth(null); // Let button keep natural size

        VerticalLayout passwordLayout = new VerticalLayout(passwordField, buttonWrapper);
        passwordLayout.setPadding(false);
        passwordLayout.setSpacing(true);
        passwordLayout.setAlignItems(Alignment.START);

        form.add(usernameField, emailField, passwordLayout, roleSelector, createButton);

        createButton.addClickListener(e -> createUser());

        // Configure the user grid
        configureUserGrid();

        content.add(title, form, userGrid);
        add(content);

        refreshGrid();
    }

    private void createUser() {
        String username = usernameField.getValue();
        String email = emailField.getValue();
        String password = passwordField.getValue();
        String role = roleSelector.getValue().equals("ADMIN") ? Roles.ADMIN : Roles.USER;

        if (email == null || email.trim().isEmpty()) {
            Notification.show("Email is required.");
            return;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            Notification.show("User already exists with this email.");
            return;
        }

        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            Notification.show("User already exists with this username.");
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRoles(Collections.singleton(role)); //

        userRepository.save(user);
        Notification.show("User created successfully.");
        clearForm();
        refreshGrid();
    }

    private void clearForm() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        roleSelector.setValue("USER");
    }

    private void configureUserGrid() {
        userGrid.removeAllColumns(); // Clear any auto columns

        userGrid.addColumn(User::getUsername).setHeader("Username");
        userGrid.addColumn(User::getEmail).setHeader("Email");
        userGrid.addColumn(user -> String.join(", ", user.getRoles()))
                .setHeader("Roles");

        userGrid.addComponentColumn(user -> {
            Button resetButton = new Button("Reset Password", e -> {
                Dialog resetDialog = new Dialog();
                resetDialog.setWidth("400px"); // Make dialog a bit larger
                resetDialog.setCloseOnEsc(true);
                resetDialog.setCloseOnOutsideClick(true);

                VerticalLayout dialogLayout = new VerticalLayout();
                dialogLayout.setSpacing(true);
                dialogLayout.setPadding(true);

                dialogLayout.add("Reset password for: " + user.getEmail());

                PasswordField newPasswordField = new PasswordField("New Password");
                newPasswordField.setRevealButtonVisible(true); // 👁 Show/hide toggle

                // Generate random password button
                Button generateButton = new Button("Generate Password", ev -> {
                    String generatedPassword = GENERAL_UTIL.generateRandomPassword(8);
                    newPasswordField.setValue(generatedPassword);
                });

                // Clipboard copy button
                Button copyButton = new Button("Copy", ev -> {
                    newPasswordField.getElement().executeJs(
                            "navigator.clipboard.writeText($0)",
                            newPasswordField.getValue()
                    );
                });

                // Confirm & Cancel buttons
                Button confirm = new Button("Confirm");
                confirm.setEnabled(false); // Initially disabled

                confirm.addClickListener(event -> {
                    String newPassword = newPasswordField.getValue();
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);
                    Notification.show("Password updated.");
                    resetDialog.close();
                });

                Button cancel = new Button("Cancel", event -> resetDialog.close());

                // Enable confirm only when not empty
                newPasswordField.addValueChangeListener(ev -> {
                    String value = ev.getValue();
                    confirm.setEnabled(value != null && !value.trim().isEmpty());
                });

                HorizontalLayout genAndCopy = new HorizontalLayout(generateButton, copyButton);
                HorizontalLayout actions = new HorizontalLayout(confirm, cancel);

                dialogLayout.add(newPasswordField, genAndCopy, actions);
                resetDialog.add(dialogLayout);
                resetDialog.open();
            });
            return resetButton;
        }).setHeader("Reset Password");

        userGrid.addComponentColumn(user -> {
            Button deleteButton = new Button("Delete", e -> {
                Dialog confirmDialog = new Dialog();
                confirmDialog.setCloseOnEsc(true);
                confirmDialog.setCloseOnOutsideClick(true);

                VerticalLayout dialogLayout = new VerticalLayout();
                dialogLayout.add("Are you sure you want to delete this user: " + user.getEmail() + "?");

                Button confirm = new Button("Yes", event -> {
                    userRepository.delete(user);
                    Notification.show("User deleted");
                    refreshGrid();
                    confirmDialog.close();
                });

                Button cancel = new Button("No", event -> confirmDialog.close());

                HorizontalLayout buttons = new HorizontalLayout(confirm, cancel);
                dialogLayout.add(buttons);

                confirmDialog.add(dialogLayout);
                confirmDialog.open();
            });
            return deleteButton;
        }).setHeader("Delete");

        int rowCount = userRepository.findAll().size();
        userGrid.setHeight((rowCount * 50 + 50) + "px"); // 50px per row + header

        userGrid.setWidthFull();
    }

    private void refreshGrid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                loggedInEmail = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                // Sometimes principal can be a String (username)
                loggedInEmail = (String) principal;
            } else {
                loggedInEmail = null;
            }
        } else {
            loggedInEmail = null;
        }

        List<User> users = userRepository.findAll();
        if (loggedInEmail != null) {
            // Filter out the currently logged-in user by email
            users = users.stream()
                    .filter(user -> !loggedInEmail.equalsIgnoreCase(user.getEmail()))
                    .toList();
        }

        System.out.println("Loaded users (excluding logged-in): " + users.size()); // DEBUG
        userGrid.setItems(users);
    }
}
