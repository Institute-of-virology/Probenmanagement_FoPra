package de.unimarburg.samplemanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
                .formLogin(Customizer.withDefaults())
                .oneTimeTokenLogin(Customizer.withDefaults())
                .build();
    }

    @Bean
    public UserDetailsService users() {
        UserDetails admin = User.builder()
                .username("admin")
                .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
                .roles(Roles.ADMIN)
                .build();
        return new InMemoryUserDetailsManager(admin);
        
    }
}

//@Configuration
//@EnableWebSecurity
//public class SecurityConfig extends VaadinWebSecurity {
//
////    @Bean
////    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////        http.csrf(AbstractHttpConfigurer::disable)
////                .cors(AbstractHttpConfigurer::disable)
////                .authorizeHttpRequests((requests)->requests
////                        .anyRequest().permitAll());
////        return http.build();
////    }
//
////    @Bean
////    SecurityFilterChain ottSecurityFilterChain(HttpSecurity http) throws Exception {
////        return http
////                .authorizeHttpRequests(ht -> ht.anyRequest().authenticated())
////                .formLogin(withDefaults())
////                .oneTimeTokenLogin(withDefaults())
////                .build();
////    }
//
//
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeHttpRequests(c -> c
//                .requestMatchers(new AntPathRequestMatcher("/images/*.png"))
//                .permitAll());
//
//        http.formLogin(withDefaults())
//                .oneTimeTokenLogin(withDefaults());
//
//        super.configure(http);
//    }
//
////    private final UserDetailService userDetailService;
////
////    public SecurityConfig(UserDetailService userDetailService) {
////        this.userDetailService = userDetailService;
////    }
////
////    @Override
////    protected void configure(HttpSecurity http) throws Exception {
////        http.authorizeHttpRequests(c -> c
////                        .requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll()
////                        .anyRequest().authenticated())
////                .formLogin(Customizer.withDefaults())
////                .logout(Customizer.withDefaults());
////
////        super.configure(http);
////    }
////
////    @Bean
////    protected UserDetailsService userDetailsService() {
////        return userDetailService;
////    }
//
//}