package com.airtribe.draftly.service.auth;

import com.airtribe.draftly.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Issues and verifies JWTs for two distinct purposes, distinguished by a
 * {@code purpose} claim so one can never be substituted for the other:
 *
 *   - "auth": a login session token, sent as {@code Authorization: Bearer ...}.
 *   - "oauth_state": a short-lived token carrying the user's email through the
 *     Gmail OAuth2 redirect. Google's callback is an unauthenticated browser
 *     request, so this is how the callback knows which Draftly user the Gmail
 *     tokens belong to.
 */
@Service
public class JwtService {

    private static final String PURPOSE_AUTH = "auth";
    private static final String PURPOSE_OAUTH_STATE = "oauth_state";
    private static final Duration OAUTH_STATE_TTL = Duration.ofMinutes(10);

    private final SecretKey key;
    private final Duration authTtl;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(props.getJwtSecret()));
        this.authTtl = Duration.ofMinutes(props.getJwtExpirationMinutes());
    }

    public String generateAuthToken(String email) {
        return generateToken(email, PURPOSE_AUTH, authTtl);
    }

    public String generateOAuthState(String email) {
        return generateToken(email, PURPOSE_OAUTH_STATE, OAUTH_STATE_TTL);
    }

    /** @return the email, if {@code token} is a valid, unexpired "auth" token. */
    public String parseAuthToken(String token) {
        return parse(token, PURPOSE_AUTH);
    }

    /** @return the email, if {@code token} is a valid, unexpired "oauth_state" token. */
    public String parseOAuthState(String token) {
        return parse(token, PURPOSE_OAUTH_STATE);
    }

    private String generateToken(String subject, String purpose, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("purpose", purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private String parse(String token, String expectedPurpose) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedPurpose.equals(claims.get("purpose", String.class))) {
            throw new JwtException("Unexpected token purpose: expected " + expectedPurpose);
        }
        return claims.getSubject();
    }
}
