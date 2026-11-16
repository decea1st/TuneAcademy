package com.tuneacademy.backend.profile;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tuneacademy.backend.profile.ProfileController.ProfileRequest;
import com.tuneacademy.backend.user.Role;
import com.tuneacademy.backend.user.SkillLevel;
import com.tuneacademy.backend.user.User;
import com.tuneacademy.backend.user.UserRepository;

@Service
public class ProfileService {

    private final UserRepository users;
    private final InstructorProfileRepository instructorProfiles;

    public ProfileService(UserRepository users, InstructorProfileRepository instructorProfiles) {
        this.users = users;
        this.instructorProfiles = instructorProfiles;
    }

    /**
     * Saves the onboarding form and marks onboarding complete. For an instructor this writes two
     * rows (users and instructor_profiles); @Transactional makes it all-or-nothing, so there's never
     * an instructor marked "onboarded" without a profile.
     */
    @Transactional
    public User save(Long userId, ProfileRequest form) {
        User user = users.findById(userId).orElseThrow();
        user.setFullName(form.fullName().trim());
        user.setBio(form.bio() == null ? "" : form.bio().trim());

        if (user.getRole() == Role.LEARNER) {
            if (form.skillLevel() == null || isEmpty(form.instruments())) {
                throw badRequest("Pick your skill level and at least one instrument.");
            }
            user.setSkillLevel(form.skillLevel());
            user.setInstruments(form.instruments().toArray(String[]::new));
        } else {
            if (form.experienceYears() == null || form.hourlyRateCents() == null || isBlank(form.nationality())
                    || isEmpty(form.specialties()) || isEmpty(form.teachingLevels())) {
                throw badRequest("Fill in your experience, nationality, hourly rate, specialties and teaching levels.");
            }
            InstructorProfile profile = instructorProfiles.findById(userId)
                .orElseGet(() -> new InstructorProfile(userId));
            profile.update(
                form.experienceYears(),
                form.nationality().trim(),
                form.specialties().toArray(String[]::new),
                form.teachingLevels().stream().map(SkillLevel::name).toArray(String[]::new),
                form.hourlyRateCents());
            instructorProfiles.save(profile);
        }

        user.setOnboardingComplete(true);
        return users.save(user);
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
