package com.tuneacademy.backend.support;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tuneacademy.backend.user.MeResponse;

/**
 * Acts like a browser against the real server: keeps cookies (the login session) between requests,
 * and copies the CSRF cookie into the X-XSRF-TOKEN header on writes, like React's apiFetch does.
 * (AttemptEndpointTest has its own older copy; it's left alone so the Practice module stays untouched.)
 */
public class TestBrowser {

    private final CookieManager cookies = new CookieManager();
    private final RestClient http;

    public TestBrowser(int port) {
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        http = RestClient.builder()
            // 127.0.0.1, not "localhost": Java's cookie store handles dotless host names oddly.
            .baseUrl("http://127.0.0.1:" + port)
            .requestFactory(new JdkClientHttpRequestFactory(client))
            .build();
    }

    // ---- Accounts ----

    /** Signs up a brand-new user with a random email, which also logs them in. */
    public void signUp(String role) {
        post("/api/auth/signup", Map.of(
            "email", role.toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
            "password", "password123",
            "fullName", "Test " + role,
            "role", role), Void.class);
    }

    public void logIn(String email, String password) {
        post("/api/auth/login", Map.of("email", email, "password", password), Void.class);
    }

    /** A new learner who has finished onboarding. */
    public MeResponse signUpOnboardedLearner(String fullName) {
        signUp("LEARNER");
        return put("/api/me/profile", Map.of(
            "fullName", fullName,
            "skillLevel", "BEGINNER",
            "instruments", List.of("Piano")), MeResponse.class);
    }

    /** A new instructor who has finished onboarding. */
    public MeResponse signUpOnboardedInstructor(String fullName) {
        signUp("INSTRUCTOR");
        return put("/api/me/profile", Map.of(
            "fullName", fullName,
            "bio", "Test instructor.",
            "experienceYears", 3,
            "nationality", "Canada",
            "specialties", List.of("Piano"),
            "teachingLevels", List.of("BEGINNER"),
            "hourlyRateCents", 3000), MeResponse.class);
    }

    public MeResponse me() {
        return get("/api/me", MeResponse.class);
    }

    // ---- Requests ----

    public <T> T get(String path, Class<T> type) {
        return http.get().uri(path).retrieve().body(type);
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return write(HttpMethod.POST, path, body).retrieve().body(type);
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return write(HttpMethod.PUT, path, body).retrieve().body(type);
    }

    /** The HTTP status of a GET, without treating 4xx as an error. */
    public int getStatus(String path) {
        return status(http.get().uri(path));
    }

    /** The HTTP status of a POST (body may be null), without treating 4xx as an error. */
    public int postStatus(String path, Object body) {
        return status(write(HttpMethod.POST, path, body));
    }

    /** The HTTP status of a PUT, without treating 4xx as an error. */
    public int putStatus(String path, Object body) {
        return status(write(HttpMethod.PUT, path, body));
    }

    private RestClient.RequestBodySpec write(HttpMethod method, String path, Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(path).header("X-XSRF-TOKEN", csrfToken());
        return body == null ? request : request.contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static int status(RestClient.RequestHeadersSpec<?> request) {
        return request.retrieve()
            .onStatus(code -> true, (req, res) -> {})
            .toBodilessEntity().getStatusCode().value();
    }

    // The first write asks Spring for a token, which sets the XSRF-TOKEN cookie; later writes reuse it.
    private String csrfToken() {
        if (findCsrfCookie() == null) {
            http.get().uri("/api/auth/csrf").retrieve().toBodilessEntity();
        }
        return findCsrfCookie();
    }

    private String findCsrfCookie() {
        return cookies.getCookieStore().getCookies().stream()
            .filter(c -> c.getName().equals("XSRF-TOKEN"))
            .map(c -> c.getValue())
            .findFirst().orElse(null);
    }
}
