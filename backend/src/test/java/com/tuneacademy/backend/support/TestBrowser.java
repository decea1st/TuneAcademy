package com.tuneacademy.backend.support;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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

    /** Signs up a brand-new user with a random email, which also logs them in. */
    public void signUp(String role) {
        post("/api/auth/signup", Map.of(
            "email", role.toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
            "password", "password123",
            "fullName", "Test " + role,
            "role", role));
    }

    public void logIn(String email, String password) {
        post("/api/auth/login", Map.of("email", email, "password", password));
    }

    public <T> T get(String path, Class<T> type) {
        return http.get().uri(path).retrieve().body(type);
    }

    /** The HTTP status of a GET, without treating 4xx as an error. */
    public int getStatus(String path) {
        return http.get().uri(path).retrieve()
            .onStatus(code -> true, (request, response) -> {})
            .toBodilessEntity().getStatusCode().value();
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return http.put().uri(path)
            .header("X-XSRF-TOKEN", csrfToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve().body(type);
    }

    /** The HTTP status of a PUT, without treating 4xx as an error. */
    public int putStatus(String path, Object body) {
        return http.put().uri(path)
            .header("X-XSRF-TOKEN", csrfToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .onStatus(code -> true, (request, response) -> {})
            .toBodilessEntity().getStatusCode().value();
    }

    private void post(String path, Object body) {
        http.post().uri(path)
            .header("X-XSRF-TOKEN", csrfToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve().toBodilessEntity();
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
