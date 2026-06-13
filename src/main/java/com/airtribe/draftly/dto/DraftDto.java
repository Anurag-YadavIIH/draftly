package com.airtribe.draftly.dto;

import com.airtribe.draftly.domain.Draft;
import com.airtribe.draftly.domain.DraftStatus;
import java.time.Instant;

/** Response shape for a reply draft. */
public record DraftDto(
        Long id,
        Long emailId,
        String threadId,
        String tone,
        String content,
        DraftStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static DraftDto from(Draft d) {
        return new DraftDto(d.getId(), d.getEmailId(), d.getThreadId(), d.getTone(),
                d.getContent(), d.getStatus(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
