package com.airtribe.draftly.controller;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.domain.User;
import com.airtribe.draftly.dto.AuthTokenResponse;
import com.airtribe.draftly.dto.LoginRequest;
import com.airtribe.draftly.dto.RegisterRequest;
import com.airtribe.draftly.exception.InvalidStateException;
import com.airtribe.draftly.repository.UserRepository;
import com.airtribe.draftly.service.CurrentUser;
import com.airtribe.draftly.service.auth.JwtService;
import com.airtribe.draftly.service.auth.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Account auth (register/login, JWT) and Gmail OAuth2 endpoints.
 *
 * In the default demo (mock Gmail) you do NOT need to connect a Google account -
 * everything works against a seeded inbox. The Gmail endpoints implement the
 * real authorization-code flow for when {@code draftly.gmail-mode=real}:
 *
 *   1. GET  /api/auth/gmail/login    -> returns Google's consent URL (carries a
 *                                       short-lived "state" token identifying
 *                                       the calling Draftly user)
 *   2. GET  /api/auth/gmail/callback -> Google redirects here with a code and
 *                                       that state; we recover the user from the
 *                                       state, exchange the code for tokens, and
 *                                       store them for that user
 *   3. POST /api/auth/logout         -> revoke and delete the caller's stored tokens
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Account login/registration (JWT) and Gmail OAuth2 connect / status / logout")
public class AuthController {

    private static final String SCOPE =
            "https://www.googleapis.com/auth/gmail.modify";

    private final AppProperties props;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    // Java's default HttpClient negotiates HTTP/2 via ALPN, which can fail
    // against oauth2.googleapis.com with "Remote host terminated the
    // handshake" in some environments; force HTTP/1.1 for the token exchange.
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthController(AppProperties props, TokenService tokenService, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.props = props;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Create a new Draftly account")
    @PostMapping("/register")
    public AuthTokenResponse register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new InvalidStateException("An account already exists for " + request.email());
        }
        User user = userRepository.save(new User(request.email(), passwordEncoder.encode(request.password())));
        return AuthTokenResponse.bearer(jwtService.generateAuthToken(user.getEmail()), user.getEmail());
    }

    @Operation(summary = "Log in to an existing Draftly account, returns a Bearer token")
    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        return AuthTokenResponse.bearer(jwtService.generateAuthToken(request.email()), request.email());
    }

    @Operation(summary = "Get the Google consent URL to connect a Gmail account")
    @GetMapping("/gmail/login")
    public Map<String, String> gmailLogin(Authentication authentication) {
        String state = jwtService.generateOAuthState(CurrentUser.email(authentication));
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + enc(props.getGmail().getClientId())
                + "&redirect_uri=" + enc(props.getGmail().getRedirectUri())
                + "&response_type=code"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&scope=" + enc(SCOPE)
                + "&state=" + enc(state);
        return Map.of("authorizationUrl", url);
    }

    @Operation(summary = "OAuth2 redirect target - exchanges the code for tokens")
    @GetMapping("/gmail/callback")
    public Map<String, String> gmailCallback(@RequestParam String code, @RequestParam String state) {
        try {
            String userEmail = jwtService.parseOAuthState(state);

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

            tokenService.storeTokens(userEmail, accessToken, refreshToken, expiresAt, SCOPE);
            return Map.of("status", "connected", "user", userEmail);
        } catch (Exception e) {
            throw new IllegalStateException("OAuth callback failed: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Check whether the caller has connected Gmail")
    @GetMapping("/status")
    public Map<String, Object> status(Authentication authentication) {
        String userEmail = CurrentUser.email(authentication);
        return Map.of(
                "gmailMode", props.getGmailMode(),
                "connected", "mock".equalsIgnoreCase(props.getGmailMode())
                        || tokenService.isConnected(userEmail),
                "note", "mock".equalsIgnoreCase(props.getGmailMode())
                        ? "Running in mock mode - no Google account required."
                        : "Real Gmail mode - connect via /api/auth/gmail/login.");
    }

    @Operation(summary = "Log out: revoke and delete the caller's stored Gmail tokens")
    @PostMapping("/logout")
    public Map<String, String> logout(Authentication authentication) {
        tokenService.revoke(CurrentUser.email(authentication));
        return Map.of("status", "logged_out");
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
