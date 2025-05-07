package com.example.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity with Postman
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // Allow login endpoint
                )
                .httpBasic(Customizer.withDefaults()); // Enables basic HTTP auth (optional)

        return http.build();
    }
}
