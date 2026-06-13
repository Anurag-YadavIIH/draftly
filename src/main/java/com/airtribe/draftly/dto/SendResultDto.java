package com.airtribe.draftly.dto;

import com.airtribe.draftly.domain.DraftStatus;

/** Result of attempting to send an approved draft. */
public record SendResultDto(
        Long draftId,
        DraftStatus status,
        String sentGmailMessageId,
        int attempts,
        String message
) {
}
