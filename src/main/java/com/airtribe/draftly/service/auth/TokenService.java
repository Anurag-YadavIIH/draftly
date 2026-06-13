package com.airtribe.draftly.service.auth;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.domain.OAuthToken;
import com.airtribe.draftly.exception.NotFoundException;
import com.airtribe.draftly.repository.OAuthTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Manages Gmail OAuth2 tokens for a user: persisting them (encrypted), handing
 * out a valid access token (refreshing automatically when expired), and revoking
 * on logout. Encryption is delegated to {@link EncryptionService} so tokens are
 * never stored in plain text.
 */
@Service
public class TokenService {

    private final OAuthTokenRepository tokenRepository;
    private final EncryptionService encryptionService;
    private final AppProperties props;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public TokenService(OAuthTokenRepository tokenRepository,
                        EncryptionService encryptionService,
                        AppProperties props) {
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
        this.props = props;
    }

    @Transactional
    public void storeTokens(String userEmail, String accessToken, String refreshToken,
                            Instant expiresAt, String scope) {
        OAuthToken token = tokenRepository.findByUserEmail(userEmail).orElseGet(OAuthToken::new);
        token.setUserEmail(userEmail);
        token.setAccessTokenEnc(encryptionService.encrypt(accessToken));
        if (refreshToken != null) {
            token.setRefreshTokenEnc(encryptionService.encrypt(refreshToken));
        }
        token.setExpiresAt(expiresAt);
        token.setScope(scope);
        tokenRepository.save(token);
    }

    /**
     * @return a currently-valid access token for the user, refreshing it first
     *         if it has expired.
     */
    @Transactional
    public String getValidAccessToken(String userEmail) {
        OAuthToken token = tokenRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("No Gmail authorization found for " + userEmail
                        + ". Please connect Gmail first."));

        if (token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return encryptionService.decrypt(token.getAccessTokenEnc());
        }
        return refresh(token);
    }

    private String refresh(OAuthToken token) {
        try {
            String refreshToken = encryptionService.decrypt(token.getRefreshTokenEnc());
            String form = "client_id=" + props.getGmail().getClientId()
                    + "&client_secret=" + props.getGmail().getClientSecret()
                    + "&refresh_token=" + refreshToken
                    + "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());

            String newAccess = node.path("access_token").asText();
            long expiresIn = node.path("expires_in").asLong(3600);
            Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

            token.setAccessTokenEnc(encryptionService.encrypt(newAccess));
            token.setExpiresAt(expiresAt);
            tokenRepository.save(token);
            return newAccess;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh Gmail token: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void revoke(String userEmail) {
        tokenRepository.deleteByUserEmail(userEmail);
    }

    public boolean isConnected(String userEmail) {
        return tokenRepository.findByUserEmail(userEmail).isPresent();
    }
}
