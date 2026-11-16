package com.tuneacademy.backend.profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.tuneacademy.backend.TestcontainersConfiguration;
import com.tuneacademy.backend.support.TestBrowser;
import com.tuneacademy.backend.user.MeResponse;
import com.tuneacademy.backend.user.Role;
import com.tuneacademy.backend.user.SkillLevel;

/** Onboarding over real HTTP, with a throwaway Postgres (which also runs the V3 demo seed). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileEndpointTest {

    @LocalServerPort
    int port;

    @Autowired
    InstructorProfileRepository instructorProfiles;

    @Test
    void learnerOnboardingSavesTheProfile() {
        TestBrowser learner = new TestBrowser(port);
        learner.signUp("LEARNER");

        MeResponse saved = learner.put("/api/me/profile", Map.of(
            "fullName", "Sam Learner",
            "skillLevel", "BEGINNER",
            "instruments", List.of("Piano", "Voice")), MeResponse.class);

        assertTrue(saved.onboardingComplete());
        assertEquals(SkillLevel.BEGINNER, saved.skillLevel());
        assertArrayEquals(new String[] {"Piano", "Voice"}, saved.instruments());
        // And it was really saved: asking "who am I?" again says the same.
        assertTrue(learner.get("/api/me", MeResponse.class).onboardingComplete());
    }

    @Test
    void instructorOnboardingCreatesTheInstructorProfile() {
        TestBrowser instructor = new TestBrowser(port);
        instructor.signUp("INSTRUCTOR");

        MeResponse saved = instructor.put("/api/me/profile", Map.of(
            "fullName", "Ines Instructor",
            "bio", "Patient guitar teacher.",
            "experienceYears", 5,
            "nationality", "Canada",
            "specialties", List.of("Guitar"),
            "teachingLevels", List.of("BEGINNER", "INTERMEDIATE"),
            "hourlyRateCents", 4500), MeResponse.class);

        assertTrue(saved.onboardingComplete());
        InstructorProfile profile = instructorProfiles.findById(saved.id()).orElseThrow();
        assertEquals(5, profile.getExperienceYears());
        assertEquals(4500, profile.getHourlyRateCents());
        assertArrayEquals(new String[] {"Guitar"}, profile.getSpecialties());
        assertArrayEquals(new String[] {"BEGINNER", "INTERMEDIATE"}, profile.getTeachingLevels());
    }

    @Test
    void instructorWithoutARateGets400AndStaysNotOnboarded() {
        TestBrowser instructor = new TestBrowser(port);
        instructor.signUp("INSTRUCTOR");

        int status = instructor.putStatus("/api/me/profile", Map.of(
            "fullName", "Ines Instructor",
            "experienceYears", 5,
            "nationality", "Canada",
            "specialties", List.of("Guitar"),
            "teachingLevels", List.of("BEGINNER"))); // no hourlyRateCents

        assertEquals(400, status);
        MeResponse me = instructor.get("/api/me", MeResponse.class);
        assertFalse(me.onboardingComplete());
        assertFalse(instructorProfiles.existsById(me.id()));
    }

    @Test
    void seededDemoInstructorCanLogIn() {
        TestBrowser demo = new TestBrowser(port);
        demo.logIn("maya@example.com", "demo-instructor");

        MeResponse me = demo.get("/api/me", MeResponse.class);
        assertEquals(Role.INSTRUCTOR, me.role());
        assertTrue(me.onboardingComplete());
        assertTrue(instructorProfiles.existsById(me.id()));
    }
}
