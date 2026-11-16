package com.tuneacademy.backend.profile;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// The public instructor directory. Guests can use it too (see SecurityConfig).
// Read-only with no rules beyond "find it", so it calls the repository directly, without a service.
@RestController
@RequestMapping("/api/instructors")
public class InstructorController {

    private final InstructorProfileRepository instructorProfiles;

    public InstructorController(InstructorProfileRepository instructorProfiles) {
        this.instructorProfiles = instructorProfiles;
    }

    @GetMapping
    public List<InstructorResponse> all() {
        return instructorProfiles.findAllPublic();
    }

    // A learner's id finds no instructor profile, so it's the same 404 as an id that doesn't exist.
    @GetMapping("/{id}")
    public InstructorResponse one(@PathVariable Long id) {
        return instructorProfiles.findPublicById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found."));
    }
}
