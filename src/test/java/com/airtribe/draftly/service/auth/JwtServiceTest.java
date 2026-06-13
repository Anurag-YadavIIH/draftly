package com.airtribe.draftly.service.auth;

import com.airtribe.draftly.config.AppProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link JwtService}. These run with no Spring context, no database
 * and no network - they verify token round-trips and the "purpose" claim that
 * keeps "auth" (login) and "oauth_state" (Gmail OAuth redirect) tokens from
 * being substituted for each other.
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new AppProperties());

    private static final String EMAIL = "demo.user@draftly.app";

    @Test
    void authTokenRoundTripsToEmail() {
        String token = jwtService.generateAuthToken(EMAIL);
        assertEquals(EMAIL, jwtService.parseAuthToken(token));
    }

    @Test
    void oauthStateRoundTripsToEmail() {
        String token = jwtService.generateOAuthState(EMAIL);
        assertEquals(EMAIL, jwtService.parseOAuthState(token));
    }

    @Test
    void authTokenIsRejectedAsOAuthState() {
        String token = jwtService.generateAuthToken(EMAIL);
        assertThrows(JwtException.class, () -> jwtService.parseOAuthState(token));
    }

    @Test
    void oauthStateIsRejectedAsAuthToken() {
        String token = jwtService.generateOAuthState(EMAIL);
        assertThrows(JwtException.class, () -> jwtService.parseAuthToken(token));
    }

    @Test
    void expiredAuthTokenIsRejected() throws InterruptedException {
        AppProperties props = new AppProperties();
        props.setJwtExpirationMinutes(0);
        JwtService shortLived = new JwtService(props);

        String token = shortLived.generateAuthToken(EMAIL);
        Thread.sleep(10);

        assertThrows(JwtException.class, () -> shortLived.parseAuthToken(token));
    }
}
