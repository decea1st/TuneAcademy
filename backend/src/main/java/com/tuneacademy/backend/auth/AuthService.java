package com.tuneacademy.backend.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tuneacademy.backend.user.Role;
import com.tuneacademy.backend.user.User;
import com.tuneacademy.backend.user.UserRepository;

@Service
public class AuthService {

    private static final String EMAIL_TAKEN = "That email is already registered. Try logging in.";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(String email, String password, String fullName, Role role) {
        String normalizedEmail = normalizeEmail(email);
        // Friendly check first, so the usual case gets a clear message.
        if (users.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_TAKEN);
        }
        // Only the hash is stored, never the password itself.
        User user = new User(normalizedEmail, passwordEncoder.encode(password), role, fullName.trim());
        try {
            return users.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two signups with the same email at the same moment: the check above passed for both,
            // but the UNIQUE rule on users.email lets only one in. The database is the real guarantee.
            throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_TAKEN);
        }
    }

    // The logged-in user's row. The session only remembers the email, so this looks up the rest.
    public User currentUser(Authentication auth) {
        return users.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // " Sam@Mail.com " and "sam@mail.com" are the same account.
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
