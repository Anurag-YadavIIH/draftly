package com.airtribe.draftly.controller;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.service.CurrentUser;
import com.airtribe.draftly.service.auth.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Gmail OAuth2 endpoints.
 *
 * In the default demo (mock Gmail) you do NOT need to connect a Google account -
 * everything works against a seeded inbox. These endpoints implement the real
 * authorization-code flow for when {@code draftly.gmail-mode=real}:
 *
 *   1. GET  /api/auth/gmail/login    -> returns Google's consent URL
 *   2. GET  /api/auth/gmail/callback -> Google redirects here with a code; we
 *                                       exchange it for tokens and store them
 *   3. POST /api/auth/logout         -> revoke and delete stored tokens
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Gmail OAuth2 connect / status / logout")
public class AuthController {

    private static final String SCOPE =
            "https://www.googleapis.com/auth/gmail.modify";

    private final AppProperties props;
    private final TokenService tokenService;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthController(AppProperties props, TokenService tokenService) {
        this.props = props;
        this.tokenService = tokenService;
    }

    @Operation(summary = "Get the Google consent URL to connect a Gmail account")
    @GetMapping("/gmail/login")
    public Map<String, String> login() {
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + enc(props.getGmail().getClientId())
                + "&redirect_uri=" + enc(props.getGmail().getRedirectUri())
                + "&response_type=code"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&scope=" + enc(SCOPE);
        return Map.of("authorizationUrl", url);
    }

    @Operation(summary = "OAuth2 redirect target - exchanges the code for tokens")
    @GetMapping("/gmail/callback")
    public Map<String, String> callback(@RequestParam String code) {
        try {
            String form = "code=" + enc(code)
                    + "&client_id=" + enc(props.getGmail().getClientId())
                    + "&client_secret=" + enc(props.getGmail().getClientSecret())
                    + "&redirect_uri=" + enc(props.getGmail().getRedirectUri())
                    + "&grant_type=authorization_code";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());

            String accessToken = node.path("access_token").asText();
            String refreshToken = node.path("refresh_token").asText(null);
            long expiresIn = node.path("expires_in").asLong(3600);
            Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

            tokenService.storeTokens(CurrentUser.EMAIL, accessToken, refreshToken, expiresAt, SCOPE);
            return Map.of("status", "connected", "user", CurrentUser.EMAIL);
        } catch (Exception e) {
            throw new IllegalStateException("OAuth callback failed: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Check whether Gmail is connected")
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "gmailMode", props.getGmailMode(),
                "connected", "mock".equalsIgnoreCase(props.getGmailMode())
                        || tokenService.isConnected(CurrentUser.EMAIL),
                "note", "mock".equalsIgnoreCase(props.getGmailMode())
                        ? "Running in mock mode - no Google account required."
                        : "Real Gmail mode - connect via /api/auth/gmail/login.");
    }

    @Operation(summary = "Log out: revoke and delete stored Gmail tokens")
    @PostMapping("/logout")
    public Map<String, String> logout() {
        tokenService.revoke(CurrentUser.EMAIL);
        return Map.of("status", "logged_out");
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
