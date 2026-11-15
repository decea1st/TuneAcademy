package com.tuneacademy.backend.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tuneacademy.backend.user.User;
import com.tuneacademy.backend.user.UserRepository;

// Tells Spring Security how to find a user at login. Spring calls loadUserByUsername
// with whatever was typed in the email box (Spring calls it "username"; ours is the email).
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmail(AuthService.normalizeEmail(email))
            .orElseThrow(() -> new UsernameNotFoundException("No user with that email"));

        // Spring's own user type (same name as ours, so it's written out in full).
        // roles("INSTRUCTOR") is what hasRole("INSTRUCTOR") in SecurityConfig checks.
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .build();
    }
}
