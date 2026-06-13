package com.airtribe.draftly.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request to generate a reply draft for a given inbound email.
 * Tone is optional; if omitted the user's default tone is used.
 */
public record GenerateDraftRequest(
        @NotNull(message = "emailId is required") Long emailId,
        String tone
) {
}
