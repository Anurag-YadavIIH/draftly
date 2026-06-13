package com.airtribe.draftly.service.gmail;

import com.airtribe.draftly.exception.SendFailedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory Gmail stand-in (the DEFAULT). It serves a seeded inbox and simulates
 * sending so the entire fetch -> draft -> approve -> send flow works with no
 * Google account. It can also simulate transient send failures to demonstrate
 * the automatic retry + idempotency machinery.
 */
@Component
@ConditionalOnProperty(name = "draftly.gmail-mode", havingValue = "mock", matchIfMissing = true)
public class MockGmailTool implements GmailTool {

    /** Per-idempotency-key remaining simulated failures. */
    private final ConcurrentHashMap<String, AtomicInteger> failureBudget = new ConcurrentHashMap<>();

    @Override
    public List<FetchedEmail> fetchRecent(String userEmail, int max) {
        Instant now = Instant.now();
        List<FetchedEmail> seeded = List.of(
                new FetchedEmail("gmail-msg-1001", "thread-1001",
                        "Priya Sharma <priya@acmecorp.com>", userEmail,
                        "Meeting request: Q3 planning sync",
                        "Hi, could we set up a 30-minute call next week to align on the Q3 roadmap? "
                                + "Tuesday or Wednesday afternoon works best for me. Let me know what suits you.",
                        now.minus(2, ChronoUnit.HOURS), true),
                new FetchedEmail("gmail-msg-1002", "thread-1002",
                        "billing@cloudhost.io", userEmail,
                        "Your invoice #INV-4821 is ready",
                        "Your monthly invoice of $42.00 is now available. Please confirm receipt or reach "
                                + "out if you have any questions about the charges.",
                        now.minus(5, ChronoUnit.HOURS), true),
                new FetchedEmail("gmail-msg-1003", "thread-1003",
                        "Rahul Verma <rahul.verma@partnerco.com>", userEmail,
                        "Following up on the integration proposal",
                        "Just following up on the proposal I sent last week. Did you get a chance to review "
                                + "the API integration scope? Happy to jump on a call if that's easier.",
                        now.minus(1, ChronoUnit.DAYS), true)
        );
        return seeded.stream().limit(max).toList();
    }

    @Override
    public SentMessage sendReply(OutboundMessage message) throws SendFailedException {
        // Demo hook: fail a few times before succeeding for a given logical send.
        if (message.injectTransientFailures() > 0) {
            String key = message.threadId() + "|" + message.to();
            AtomicInteger remaining = failureBudget.computeIfAbsent(
                    key, k -> new AtomicInteger(message.injectTransientFailures()));
            if (remaining.getAndDecrement() > 0) {
                throw new SendFailedException(
                        "Simulated transient Gmail failure (token refresh in progress)");
            }
        }

        // "Send" succeeds: return a fake Gmail message id, keeping the thread id.
        String sentId = "sent-" + UUID.randomUUID().toString().substring(0, 8);
        return new SentMessage(sentId, message.threadId());
    }
}
