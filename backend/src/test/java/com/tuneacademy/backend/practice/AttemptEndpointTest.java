package com.tuneacademy.backend.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.tuneacademy.backend.TestcontainersConfiguration;
import com.tuneacademy.backend.practice.PracticeController.AttemptResponse;

/**
 * Starts the real app on a random port, with a throwaway Postgres, and talks to it over real HTTP
 * the way the browser does: session cookie, CSRF header, multipart upload.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttemptEndpointTest {

    // If an upload were ever written to disk, it would land in this folder.
    private static final Path UPLOAD_DIR = createTempDir();

    @DynamicPropertySource
    static void uploadFolder(DynamicPropertyRegistry registry) {
        registry.add("spring.servlet.multipart.location", UPLOAD_DIR::toString);
    }

    @LocalServerPort
    int port;

    @Autowired
    ExerciseRepository exercises;

    @Autowired
    AttemptRepository attempts;

    @Test
    void uploadingTheReferenceScores100AndKeepsNoAudio() throws IOException {
        Browser student = new Browser(port);
        student.signUp();
        Long exerciseId = youSayRun().getId();

        // The reference played back perfectly: the known answer is 100.
        AttemptResponse result = student.upload(exerciseId, referenceWav());

        assertEquals(100, result.totalScore());
        assertEquals(100, result.timingScore());
        assertEquals(100, result.noteScore());

        Attempt saved = attempts.findById(result.id()).orElseThrow();
        assertEquals(exerciseId, saved.getExerciseId());
        assertEquals(100, saved.getTotalScore());

        try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
            assertEquals(0, files.count(), "no audio file left on disk");
        }
    }

    @Test
    void anotherUserGets404ForMyAttempt() throws IOException {
        Browser owner = new Browser(port);
        owner.signUp();
        AttemptResponse mine = owner.upload(youSayRun().getId(), referenceWav());

        Browser someoneElse = new Browser(port);
        someoneElse.signUp();

        assertEquals(200, owner.status("/api/attempts/" + mine.id()));
        assertEquals(404, someoneElse.status("/api/attempts/" + mine.id()));
    }

    private Exercise youSayRun() {
        return exercises.findAll().stream()
            .filter(e -> e.getTitle().startsWith("You Say Run"))
            .findFirst().orElseThrow();
    }

    private static byte[] referenceWav() throws IOException {
        try (InputStream in = AttemptEndpointTest.class.getResourceAsStream("/exercises/you-say-run-melody.wav")) {
            return in.readAllBytes();
        }
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("upload-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Acts like a browser: keeps cookies between requests and copies the CSRF cookie into a header, like React does. */
    static class Browser {

        private final CookieManager cookies = new CookieManager();
        private final RestClient http;

        Browser(int port) {
            HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
            http = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(client))
                .build();
        }

        void signUp() {
            http.get().uri("/api/auth/csrf").retrieve().toBodilessEntity();
            http.post().uri("/api/auth/signup")
                .header("X-XSRF-TOKEN", csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "email", "student-" + UUID.randomUUID() + "@example.com",
                    "password", "password123",
                    "fullName", "Test Student",
                    "role", "LEARNER"))
                .retrieve().toBodilessEntity();
        }

        AttemptResponse upload(Long exerciseId, byte[] wav) {
            // One form field named "audio" holding the file, like the browser's FormData.
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentDisposition(ContentDisposition.formData().name("audio").filename("attempt.wav").build());
            fileHeaders.setContentType(MediaType.parseMediaType("audio/wav"));
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("audio", new HttpEntity<>(wav, fileHeaders));

            return http.post().uri("/api/exercises/{id}/attempts", exerciseId)
                .header("X-XSRF-TOKEN", csrfToken())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve().body(AttemptResponse.class);
        }

        /** The HTTP status of a GET, without treating 4xx as an error. */
        int status(String path) {
            return http.get().uri(path).retrieve()
                .onStatus(code -> true, (request, response) -> {})
                .toBodilessEntity().getStatusCode().value();
        }

        private String csrfToken() {
            return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN"))
                .findFirst().orElseThrow().getValue();
        }
    }
}
