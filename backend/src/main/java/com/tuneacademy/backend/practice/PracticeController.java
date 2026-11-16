package com.tuneacademy.backend.practice;

import java.time.Instant;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.tuneacademy.backend.auth.AuthService;

// Every endpoint here needs a login: SecurityConfig's anyRequest().authenticated() covers them.
@RestController
@RequestMapping("/api")
public class PracticeController {

    // The JSON shapes React receives.

    public record ExerciseResponse(Long id, String title, String instrument, boolean monophonic,
                                   String attribution, boolean available) {
        static ExerciseResponse from(Exercise e) {
            return new ExerciseResponse(e.getId(), e.getTitle(), e.getInstrument(), e.isMonophonic(),
                e.getAttribution(), e.isAvailable());
        }
    }

    /** One attempt with all its details, for the result page. */
    public record AttemptResponse(Long id, Long exerciseId, int totalScore, int timingScore, Integer noteScore,
                                  AttemptAnalysis analysis, Instant createdAt) {
        static AttemptResponse from(Attempt a) {
            return new AttemptResponse(a.getId(), a.getExerciseId(), a.getTotalScore(), a.getTimingScore(),
                a.getNoteScore(), a.getAnalysis(), a.getCreatedAt());
        }
    }

    /** Just the scores, for the "my past scores" list. */
    public record AttemptSummary(Long id, int totalScore, int timingScore, Integer noteScore, Instant createdAt) {
        static AttemptSummary from(Attempt a) {
            return new AttemptSummary(a.getId(), a.getTotalScore(), a.getTimingScore(), a.getNoteScore(), a.getCreatedAt());
        }
    }

    private final PracticeService practice;
    private final AuthService authService;

    public PracticeController(PracticeService practice, AuthService authService) {
        this.practice = practice;
        this.authService = authService;
    }

    @GetMapping("/exercises")
    public List<ExerciseResponse> exercises() {
        return practice.allExercises().stream().map(ExerciseResponse::from).toList();
    }

    // The reference recording, for the "Play reference" button.
    @GetMapping("/exercises/{id}/audio")
    public ResponseEntity<Resource> referenceAudio(@PathVariable Long id) {
        Exercise exercise = practice.findExercise(id);
        if (exercise.getAudioPath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This exercise has no audio yet.");
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/wav"))
            .body(new ClassPathResource(exercise.getAudioPath()));
    }

    // React sends the recording as a form upload with one file field named "audio".
    @PostMapping(path = "/exercises/{id}/attempts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResponse submitAttempt(@PathVariable Long id, @RequestParam("audio") MultipartFile audio,
                                         Authentication auth) {
        return AttemptResponse.from(practice.scoreAttempt(userId(auth), id, audio));
    }

    @GetMapping("/attempts/{id}")
    public AttemptResponse attempt(@PathVariable Long id, Authentication auth) {
        return AttemptResponse.from(practice.findMyAttempt(userId(auth), id));
    }

    @GetMapping("/exercises/{id}/attempts")
    public List<AttemptSummary> myAttempts(@PathVariable Long id, Authentication auth) {
        return practice.myAttempts(userId(auth), id).stream().map(AttemptSummary::from).toList();
    }

    private Long userId(Authentication auth) {
        return authService.currentUser(auth).getId();
    }
}
