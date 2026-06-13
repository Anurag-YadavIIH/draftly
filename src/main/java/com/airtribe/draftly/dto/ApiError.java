package com.airtribe.draftly.dto;

import java.time.Instant;

/** Standard error body returned by the global exception handler. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
