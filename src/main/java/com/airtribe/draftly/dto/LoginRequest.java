package com.airtribe.draftly.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to log in to an existing Draftly account. */
public record LoginRequest(
        @NotBlank(message = "email is required") String email,
        @NotBlank(message = "password is required") String password
) {
}
