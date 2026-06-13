package com.airtribe.draftly.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to edit a draft's body before approving/sending. */
public record EditDraftRequest(
        @NotBlank(message = "content must not be blank") String content
) {
}
