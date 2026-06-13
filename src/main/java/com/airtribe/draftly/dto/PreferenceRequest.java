package com.airtribe.draftly.dto;

/** Request to create/update user preferences. */
public record PreferenceRequest(
        String signature,
        String defaultTone
) {
}
