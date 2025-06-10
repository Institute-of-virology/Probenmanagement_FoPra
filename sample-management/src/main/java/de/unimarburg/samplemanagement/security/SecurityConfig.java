package de.unimarburg.samplemanagement.security;

import de.unimarburg.samplemanagement.repository.UserRepository;
import de.unimarburg.samplemanagement.service.LoginUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain ottSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/images/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                .usernameParameter("email")
                .permitAll())
                .oneTimeTokenLogin(Customizer.withDefaults())
                .build();
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        return new LoginUserDetailsService(userRepository);
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
