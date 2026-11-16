package com.tuneacademy.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Who can reach what. Checked top to bottom; the first match wins.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                // The public instructor directory: guests can browse it. Careful: "instructors" (plural)
                // is public, "instructor" (below) is instructor-only. "/*" opens exactly one level,
                // so anything added deeper under /api/instructors/ still needs a login.
                .requestMatchers(HttpMethod.GET, "/api/instructors", "/api/instructors/*").permitAll()
                // Convention: every instructor-only endpoint lives under /api/instructor/.
                .requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
                .requestMatchers("/api/**").authenticated()
                // Everything else is the React app (pages, scripts, images), which guests load too.
                // Its data still comes from /api, so this opens no data.
                .anyRequest().permitAll()
            )
            // CSRF protection for a React app: Spring puts a random token in a cookie that
            // JavaScript can read (XSRF-TOKEN), and every POST/PUT/DELETE must send it back
            // in the X-XSRF-TOKEN header. Another website can't read our cookies, so it can't
            // forge a request that includes the right header.
            .csrf(csrf -> csrf.spa())
            // Not logged in -> plain 401. No redirect to a login page; React decides what to show.
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // POST /api/auth/logout ends the session and answers 204 (success, no body).
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            );
        return http.build();
    }

    // Hashes passwords on signup and checks them on login. BCrypt is deliberately slow,
    // so a stolen database can't be cracked quickly.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // What login calls to check an email + password: load the user (UserDetailsService),
    // then compare the password with the stored hash (PasswordEncoder).
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
