package com.airtribe.draftly.dto;

/** Response shape for register/login: a Bearer token to use on subsequent requests. */
public record AuthTokenResponse(
        String token,
        String tokenType,
        String email
) {
    public static AuthTokenResponse bearer(String token, String email) {
        return new AuthTokenResponse(token, "Bearer", email);
    }
}
