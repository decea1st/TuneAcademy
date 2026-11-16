package com.tuneacademy.backend.tutoring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.tuneacademy.backend.TestcontainersConfiguration;
import com.tuneacademy.backend.support.TestBrowser;
import com.tuneacademy.backend.tutoring.TutoringController.IncomingRequest;
import com.tuneacademy.backend.tutoring.TutoringController.MyRequest;
import com.tuneacademy.backend.user.MeResponse;
import com.tuneacademy.backend.user.SkillLevel;

/** Tutoring requests over real HTTP. Every test makes its own new learner and instructor. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TutoringRequestEndpointTest {

    @LocalServerPort
    int port;

    @Autowired
    EngagementRepository engagements;

    @Test
    void instructorSeesTheRequestWithLearnerDetailsButNoEmail() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser learner = new TestBrowser(port);
        learner.signUpOnboardedLearner("Leo Learner");

        MyRequest sent = learner.post("/api/requests", body(instructorMe, "Hi! Piano lessons, please."), MyRequest.class);
        assertEquals(RequestStatus.PENDING, sent.status());
        assertEquals("Ivy Instructor", sent.instructorName());

        IncomingRequest incoming = instructor.get("/api/requests/incoming", IncomingRequest[].class)[0];
        assertEquals("Leo Learner", incoming.learnerName());
        assertEquals(SkillLevel.BEGINNER, incoming.learnerSkillLevel());
        assertArrayEquals(new String[] {"Piano"}, incoming.learnerInstruments());
        assertEquals("Hi! Piano lessons, please.", incoming.message());

        // Not accepted yet: no email on either side. Checks the raw JSON, so nothing can slip out.
        assertFalse(instructor.get("/api/requests/incoming", String.class).contains("@"));
        assertFalse(learner.get("/api/requests/mine", String.class).contains("@"));
    }

    @Test
    void secondPendingRequestToTheSameInstructorGets409() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser learner = new TestBrowser(port);
        learner.signUpOnboardedLearner("Leo Learner");

        assertEquals(201, learner.postStatus("/api/requests", body(instructorMe, "First")));
        assertEquals(409, learner.postStatus("/api/requests", body(instructorMe, "Second")));
    }

    @Test
    void instructorsCannotSendRequests() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser otherInstructor = new TestBrowser(port);
        otherInstructor.signUpOnboardedInstructor("Oscar Instructor");

        assertEquals(403, otherInstructor.postStatus("/api/requests", body(instructorMe, "Hello")));
    }

    @Test
    void onlyTheRequestedInstructorCanAnswer() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser learner = new TestBrowser(port);
        learner.signUpOnboardedLearner("Leo Learner");
        MyRequest sent = learner.post("/api/requests", body(instructorMe, "Hello"), MyRequest.class);

        TestBrowser otherInstructor = new TestBrowser(port);
        otherInstructor.signUpOnboardedInstructor("Oscar Instructor");

        assertEquals(404, otherInstructor.postStatus("/api/requests/" + sent.id() + "/accept", null));
        assertEquals(404, otherInstructor.postStatus("/api/requests/" + sent.id() + "/decline", null));
        assertEquals(404, learner.postStatus("/api/requests/" + sent.id() + "/accept", null));
        assertFalse(engagements.existsById(sent.id()));
    }

    @Test
    void acceptingCreatesTheEngagementAndSharesBothEmails() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser learner = new TestBrowser(port);
        MeResponse learnerMe = learner.signUpOnboardedLearner("Leo Learner");
        MyRequest sent = learner.post("/api/requests", body(instructorMe, "Hello"), MyRequest.class);

        IncomingRequest accepted = instructor.post("/api/requests/" + sent.id() + "/accept", null, IncomingRequest.class);

        assertEquals(RequestStatus.ACCEPTED, accepted.status());
        assertTrue(engagements.existsById(sent.id()));
        // Each side now sees the other's email.
        assertEquals(learnerMe.email(), accepted.learnerEmail());
        MyRequest mine = learner.get("/api/requests/mine", MyRequest[].class)[0];
        assertEquals(RequestStatus.ACCEPTED, mine.status());
        assertEquals(instructorMe.email(), mine.instructorEmail());
        // And it can't be answered twice.
        assertEquals(409, instructor.postStatus("/api/requests/" + sent.id() + "/decline", null));
    }

    @Test
    void decliningKeepsEmailsHiddenAndCreatesNoEngagement() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser learner = new TestBrowser(port);
        learner.signUpOnboardedLearner("Leo Learner");
        MyRequest sent = learner.post("/api/requests", body(instructorMe, "Hello"), MyRequest.class);

        IncomingRequest declined = instructor.post("/api/requests/" + sent.id() + "/decline", null, IncomingRequest.class);

        assertEquals(RequestStatus.DECLINED, declined.status());
        assertNull(declined.learnerEmail());
        assertFalse(engagements.existsById(sent.id()));
        MyRequest mine = learner.get("/api/requests/mine", MyRequest[].class)[0];
        assertEquals(RequestStatus.DECLINED, mine.status());
        assertNull(mine.instructorEmail());
        // Declined requests drop off the instructor's panel.
        assertTrue(Arrays.stream(instructor.get("/api/requests/incoming", IncomingRequest[].class))
            .noneMatch(r -> r.id().equals(sent.id())));
    }

    private static Map<String, Object> body(MeResponse instructor, String message) {
        return Map.of("instructorId", instructor.id(), "message", message);
    }
}
