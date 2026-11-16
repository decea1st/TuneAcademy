package com.tuneacademy.backend.profile;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tuneacademy.backend.auth.AuthService;
import com.tuneacademy.backend.user.MeResponse;
import com.tuneacademy.backend.user.SkillLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@RestController
public class ProfileController {

    /**
     * The onboarding form. One shape for both roles: learners fill in the learner fields, instructors
     * the instructor ones, and the fields for the other role are ignored. Which fields are required
     * depends on the role, so ProfileService checks that part.
     */
    public record ProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 300) String bio,
        // Learners
        SkillLevel skillLevel,
        List<@NotBlank String> instruments,
        // Instructors
        @PositiveOrZero Integer experienceYears,
        @Size(max = 100) String nationality,
        List<@NotBlank String> specialties,
        List<SkillLevel> teachingLevels,
        @PositiveOrZero Integer hourlyRateCents
    ) {}

    private final ProfileService profileService;
    private final AuthService authService;

    public ProfileController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    // Answers with the same shape as GET /api/me, so React can just replace who's logged in.
    @PutMapping("/api/me/profile")
    public MeResponse saveProfile(@Valid @RequestBody ProfileRequest body, Authentication auth) {
        Long userId = authService.currentUser(auth).getId();
        return MeResponse.from(profileService.save(userId, body));
    }
}
