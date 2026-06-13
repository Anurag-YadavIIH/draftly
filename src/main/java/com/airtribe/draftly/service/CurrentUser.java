package com.airtribe.draftly.service;

/**
 * For this capstone we operate as a single demo user. In a multi-tenant system
 * this value would come from the authenticated principal (the logged-in user).
 */
public final class CurrentUser {

    public static final String EMAIL = "demo.user@draftly.app";

    private CurrentUser() {
    }
}
