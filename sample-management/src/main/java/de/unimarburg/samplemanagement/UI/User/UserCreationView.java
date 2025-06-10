package de.unimarburg.samplemanagement.UI.User;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import de.unimarburg.samplemanagement.utils.SIDEBAR_FACTORY;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Route("/add_user")
public class UserCreationView extends HorizontalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final TextField usernameField = new TextField("Username");
    private final EmailField emailField = new EmailField("Email");
    private final PasswordField passwordField = new PasswordField("Password");
    private final RadioButtonGroup<String> roleSelector = new RadioButtonGroup<>();

    private final Button createButton = new Button("Create User");

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
        form.add(usernameField, emailField, passwordField, roleSelector, createButton);

        createButton.addClickListener(e -> createUser());

        content.add(title, form);
        add(content);
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

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRoles(Collections.singleton(role)); //

        userRepository.save(user);
        Notification.show("User created successfully.");
        clearForm();
    }

    private void clearForm() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        roleSelector.setValue("USER");
    }
}
