package de.unimarburg.samplemanagement.config;

import de.unimarburg.samplemanagement.model.User;
import de.unimarburg.samplemanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Configuration
public class UserSeeder {

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("immunlab@staff.uni-marburg.de").isEmpty()) {
                User admin = new User();
                admin.setUsername("staffadmin");
                admin.setEmail("immunlab@staff.uni-marburg.de");
                admin.setPassword(passwordEncoder.encode("virologystaff@marburg"));
                admin.setEnabled(true);
                admin.setRoles(Collections.singleton("ADMIN"));
                userRepository.save(admin);
                System.out.println("Default admin user created.");
            }
            if (userRepository.findByEmail("blazebnayak@gmail.com").isEmpty()) {
                User user = new User();
                user.setUsername("binayak");
                user.setEmail("blazebnayak@gmail.com");
                user.setPassword(passwordEncoder.encode("Test@123"));
                user.setEnabled(true);
                user.setRoles(Collections.singleton("USER"));
                userRepository.save(user);
                System.out.println("Default user created.");
            }
        };
    }
}
