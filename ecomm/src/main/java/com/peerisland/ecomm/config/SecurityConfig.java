package com.peerisland.ecomm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security configuration.
 *
 * <p>Signup, the landing page, the H2 console and error endpoint are public;
 * everything else requires authentication via the default form-login page.
 * Authentication is backed by {@link CustomUserDetailsService} together with
 * the {@link PasswordEncoder} bean below.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled so the JSON fetch() calls in index.html work without a token.
            // H2 console is also exempt below; revisit for a production deployment.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/signup", "/api/signup", "/index.html", "/", "/h2-console/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            // Allow the H2 console to render inside a frame.
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }
}
