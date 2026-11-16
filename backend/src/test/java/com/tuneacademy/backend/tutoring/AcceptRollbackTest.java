package com.tuneacademy.backend.tutoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.tuneacademy.backend.TestcontainersConfiguration;
import com.tuneacademy.backend.support.TestBrowser;
import com.tuneacademy.backend.tutoring.TutoringController.MyRequest;
import com.tuneacademy.backend.user.MeResponse;

/**
 * Accepting a request writes two tables. This forces the second write (the engagement) to fail
 * and checks that the first (the request becoming ACCEPTED) was rolled back with it.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcceptRollbackTest {

    @LocalServerPort
    int port;

    // The real repository, wrapped so this test can make one method fail on purpose.
    @MockitoSpyBean
    EngagementRepository engagements;

    @Autowired
    TutoringService tutoring;

    @Autowired
    TutoringRequestRepository requests;

    @Test
    void failureWhileCreatingTheEngagementRollsBackTheAccept() {
        TestBrowser instructor = new TestBrowser(port);
        MeResponse instructorMe = instructor.signUpOnboardedInstructor("Ivy Instructor");
        TestBrowser learner = new TestBrowser(port);
        learner.signUpOnboardedLearner("Leo Learner");
        MyRequest sent = learner.post("/api/requests",
            Map.of("instructorId", instructorMe.id(), "message", "Hello"), MyRequest.class);

        // As if the database broke at exactly the moment the engagement is saved.
        doThrow(new IllegalStateException("simulated failure")).when(engagements).save(any(Engagement.class));

        assertThrows(IllegalStateException.class, () -> tutoring.accept(instructorMe.id(), sent.id()));

        // All or nothing: the request is still PENDING, and there's no engagement.
        assertEquals(RequestStatus.PENDING, requests.findById(sent.id()).orElseThrow().getStatus());
        assertFalse(engagements.existsById(sent.id()));
    }
}
