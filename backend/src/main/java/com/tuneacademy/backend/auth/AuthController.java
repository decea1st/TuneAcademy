package com.tuneacademy.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tuneacademy.backend.user.MeResponse;
import com.tuneacademy.backend.user.Role;
import com.tuneacademy.backend.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Logout isn't here: Spring Security handles POST /api/auth/logout itself (see SecurityConfig).
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // The JSON bodies React sends. @Valid on the method parameter checks these rules
    // and answers 400 before the method runs if any fail.
    public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password, // BCrypt only uses the first 72 bytes
        @NotBlank @Size(max = 100) String fullName,
        @NotNull Role role
    ) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    // Saves "who is logged in" into the session, so the next requests know.
    private final SecurityContextRepository sessionRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    // React calls this once before its first POST. Reading the token makes Spring send
    // the XSRF-TOKEN cookie, which React then copies into the X-XSRF-TOKEN header.
    @GetMapping("/csrf")
    public void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/signup")
    public MeResponse signup(@Valid @RequestBody SignupRequest body, HttpServletRequest request, HttpServletResponse response) {
        User user = authService.signup(body.email(), body.password(), body.fullName(), body.role());
        // Log the new user straight in, the same way /login does.
        startSession(authenticate(user.getEmail(), body.password()), request, response);
        return MeResponse.from(user);
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticate(body.email(), body.password());
        startSession(auth, request, response);
        return MeResponse.from(authService.currentUser(auth));
    }

    // Checks the email + password. Same message whether the email or the password was wrong,
    // so nobody can use login to find out which emails have accounts.
    private Authentication authenticate(String email, String password) {
        try {
            return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(AuthService.normalizeEmail(email), password));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password.");
        }
    }

    // Marks this browser as logged in. Tomcat sends the session id back in the JSESSIONID cookie,
    // and the browser includes that cookie on every later request.
    private void startSession(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        // Give the session a new id at login, so an id someone planted beforehand is useless.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        sessionRepository.saveContext(context, request, response);
    }
}
