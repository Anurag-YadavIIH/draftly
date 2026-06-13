package com.airtribe.draftly.dto;

import com.airtribe.draftly.domain.EmailMessage;
import java.time.Instant;

/** Response shape for an inbound email. */
public record EmailDto(
        Long id,
        String gmailMessageId,
        String threadId,
        String sender,
        String subject,
        String body,
        Instant receivedAt,
        boolean unread
) {
    public static EmailDto from(EmailMessage e) {
        return new EmailDto(e.getId(), e.getGmailMessageId(), e.getThreadId(), e.getSender(),
                e.getSubject(), e.getBody(), e.getReceivedAt(), e.isUnread());
    }
}
