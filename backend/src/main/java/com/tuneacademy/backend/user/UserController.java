package com.tuneacademy.backend.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuneacademy.backend.auth.AuthService;

@RestController
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    // "Who am I?" React calls this when the page loads. Logged in: your details.
    // Not logged in: 401 (SecurityConfig blocks it before it gets here).
    // Spring fills in `auth` from the session cookie.
    @GetMapping("/api/me")
    public MeResponse me(Authentication auth) {
        return MeResponse.from(authService.currentUser(auth));
    }
}
