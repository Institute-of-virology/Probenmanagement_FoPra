package de.unimarburg.samplemanagement.UI.analyses;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.unimarburg.samplemanagement.model.User;
import de.unimarburg.samplemanagement.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Route("admin/create-user")
public class UserCreationView extends VerticalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final TextField usernameField = new TextField("Username");
    private final TextField emailField = new TextField("Email");
    private final PasswordField passwordField = new PasswordField("Password");

    private final Button createButton = new Button("Create User");

    public UserCreationView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        add(new H2("Create New User"));

        FormLayout form = new FormLayout();
        form.add(usernameField, emailField, passwordField, createButton);
        add(form);

        createButton.addClickListener(e -> createUser());
    }

    private void createUser() {
        // Check if current user has ADMIN role
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Only admins can create users");
        }

        // Simple validation
        if (usernameField.isEmpty() || emailField.isEmpty() || passwordField.isEmpty()) {
            Notification.show("Please fill all fields");
            return;
        }

        // Check if email already exists
        if (userRepository.findByEmail(emailField.getValue()).isPresent()) {
            Notification.show("Email already registered");
            return;
        }

        User newUser = new User();
        newUser.setUsername(usernameField.getValue());
        newUser.setEmail(emailField.getValue());
        newUser.setPassword(passwordEncoder.encode(passwordField.getValue()));
        newUser.setEnabled(true);
        newUser.setRoles(Collections.singleton("USER")); // default role

        userRepository.save(newUser);
        Notification.show("User created successfully");

        // Clear fields
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
    }
}
