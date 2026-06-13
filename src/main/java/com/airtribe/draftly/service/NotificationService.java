package com.airtribe.draftly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Notifies the user when something needs their attention - most importantly when
 * a send has failed permanently (e.g. an expired token that needs re-auth).
 *
 * For the capstone this logs a clear, user-facing message. In production this is
 * where you would plug in email, push, or in-app notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyPersistentFailure(String userEmail, Long draftId, String reason) {
        log.warn("[NOTIFY -> {}] Draft {} could not be sent after multiple attempts. Reason: {}. "
                + "Action needed: please reconnect Gmail or try again.", userEmail, draftId, reason);
    }
}
