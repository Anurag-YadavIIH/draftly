package com.airtribe.draftly.dto;

import com.airtribe.draftly.domain.UserPreference;

/** Response shape for user preferences. */
public record PreferenceDto(
        String userEmail,
        String signature,
        String defaultTone
) {
    public static PreferenceDto from(UserPreference p) {
        return new PreferenceDto(p.getUserEmail(), p.getSignature(), p.getDefaultTone());
    }
}
