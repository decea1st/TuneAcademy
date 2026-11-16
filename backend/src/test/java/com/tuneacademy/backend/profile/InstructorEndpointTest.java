package com.tuneacademy.backend.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.tuneacademy.backend.TestcontainersConfiguration;
import com.tuneacademy.backend.support.TestBrowser;
import com.tuneacademy.backend.user.MeResponse;

/**
 * The public instructor directory, over real HTTP. The database is shared with the other tests,
 * so the list can contain more instructors than the three seeded ones.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InstructorEndpointTest {

    @LocalServerPort
    int port;

    @Test
    void guestSeesTheSeededInstructors() {
        TestBrowser guest = new TestBrowser(port); // never logs in

        InstructorResponse[] list = guest.get("/api/instructors", InstructorResponse[].class);

        List<String> names = Arrays.stream(list).map(InstructorResponse::fullName).toList();
        assertTrue(names.containsAll(List.of("Maya Chen", "Diego Alvarez", "Amara Okafor")), names.toString());
        InstructorResponse maya = Arrays.stream(list).filter(i -> i.fullName().equals("Maya Chen")).findFirst().orElseThrow();
        assertEquals(4500, maya.hourlyRateCents());
    }

    @Test
    void noEmailIsEverSent() {
        TestBrowser guest = new TestBrowser(port);
        String listJson = guest.get("/api/instructors", String.class);
        Long mayaId = Arrays.stream(guest.get("/api/instructors", InstructorResponse[].class))
            .filter(i -> i.fullName().equals("Maya Chen")).findFirst().orElseThrow().id();
        String detailJson = guest.get("/api/instructors/" + mayaId, String.class);

        // Checks the raw JSON, so an email can't slip out under any field name.
        for (String json : List.of(listJson, detailJson)) {
            assertFalse(json.contains("email"), json);
            assertFalse(json.contains("@"), json);
        }
    }

    @Test
    void detailPageFindsOneInstructor() {
        TestBrowser guest = new TestBrowser(port);
        Long mayaId = Arrays.stream(guest.get("/api/instructors", InstructorResponse[].class))
            .filter(i -> i.fullName().equals("Maya Chen")).findFirst().orElseThrow().id();

        InstructorResponse maya = guest.get("/api/instructors/" + mayaId, InstructorResponse.class);
        assertEquals("Maya Chen", maya.fullName());
        assertEquals("Canada", maya.nationality());
    }

    @Test
    void unknownIdAndLearnerIdGet404() {
        TestBrowser learner = new TestBrowser(port);
        learner.signUp("LEARNER");
        Long learnerId = learner.get("/api/me", MeResponse.class).id();

        assertEquals(404, learner.getStatus("/api/instructors/999999"));
        assertEquals(404, learner.getStatus("/api/instructors/" + learnerId));
    }
}
