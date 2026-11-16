package com.tuneacademy.backend.tutoring;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tuneacademy.backend.auth.AuthService;
import com.tuneacademy.backend.user.SkillLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Every endpoint here needs a login: SecurityConfig's anyRequest().authenticated() covers them.
// Who may do what (learners send, only the right instructor answers) is checked in TutoringService.
@RestController
@RequestMapping("/api/requests")
public class TutoringController {

    public record SendRequest(@NotNull Long instructorId, @NotBlank @Size(max = 500) String message) {}

    /** What the instructor sees. learnerEmail stays null until the request is accepted. */
    public record IncomingRequest(Long id, String learnerName, SkillLevel learnerSkillLevel,
                                  String[] learnerInstruments, String message, RequestStatus status,
                                  Instant createdAt, String learnerEmail) {}

    /** What the learner sees about their own request. instructorEmail stays null until it's accepted. */
    public record MyRequest(Long id, Long instructorId, String instructorName, String message,
                            RequestStatus status, Instant createdAt, String instructorEmail) {}

    private final TutoringService tutoring;
    private final AuthService authService;

    public TutoringController(TutoringService tutoring, AuthService authService) {
        this.tutoring = tutoring;
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyRequest send(@Valid @RequestBody SendRequest body, Authentication auth) {
        return tutoring.send(authService.currentUser(auth), body.instructorId(), body.message());
    }

    @GetMapping("/incoming")
    public List<IncomingRequest> incoming(Authentication auth) {
        return tutoring.incoming(userId(auth));
    }

    @GetMapping("/mine")
    public List<MyRequest> mine(Authentication auth) {
        return tutoring.mine(userId(auth));
    }

    @PostMapping("/{id}/accept")
    public IncomingRequest accept(@PathVariable Long id, Authentication auth) {
        return tutoring.accept(userId(auth), id);
    }

    @PostMapping("/{id}/decline")
    public IncomingRequest decline(@PathVariable Long id, Authentication auth) {
        return tutoring.decline(userId(auth), id);
    }

    private Long userId(Authentication auth) {
        return authService.currentUser(auth).getId();
    }
}
