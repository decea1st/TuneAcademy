package com.tuneacademy.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.tuneacademy.backend.TestcontainersConfiguration;
import com.tuneacademy.backend.support.TestBrowser;

/**
 * Spring serving the React app, as it does in production.
 * src/test/resources/static/index.html stands in for the real React build.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpaFallbackTest {

    private static final String PAGE = "test-react-app";

    @LocalServerPort
    int port;

    @Test
    void guestCanOpenTheHomePageAndADeepLink() {
        TestBrowser guest = new TestBrowser(port); // never logs in

        assertTrue(guest.get("/", String.class).contains(PAGE));
        assertTrue(guest.get("/app/practice/3", String.class).contains(PAGE));
    }

    @Test
    void theApiStillNeedsALogin() {
        TestBrowser guest = new TestBrowser(port);

        assertEquals(401, guest.getStatus("/api/me"));
        assertEquals(401, guest.getStatus("/api/does-not-exist"));
    }

    @Test
    void unknownApiPathsAndMissingFilesAre404NotThePage() {
        TestBrowser learner = new TestBrowser(port);
        learner.signUp("LEARNER");

        assertEquals(404, learner.getStatus("/api/does-not-exist"));
        assertEquals(404, learner.getStatus("/assets/missing.js"));
    }
}
