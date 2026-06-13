package com.airtribe.draftly.service;

import org.springframework.security.core.Authentication;

/**
 * Resolves the authenticated user's identity. Every entity in this app is
 * keyed by {@code userEmail}, and {@link Authentication#getName()} returns
 * exactly that email (the JWT subject is the user's email).
 */
public final class CurrentUser {

    public static String email(Authentication authentication) {
        return authentication.getName();
    }

    private CurrentUser() {
    }
}
