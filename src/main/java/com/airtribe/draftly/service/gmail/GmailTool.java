package com.airtribe.draftly.service.gmail;

import com.airtribe.draftly.exception.SendFailedException;

import java.time.Instant;
import java.util.List;

/**
 * A tool abstraction over Gmail, in the spirit of the Model Context Protocol
 * (MCP): the agent interacts with the outside world only through a small, well
 * defined set of "tools" (here: fetch inbox, send reply). This keeps the agent
 * logic decoupled from any specific provider.
 *
 * Two implementations exist, selected by {@code draftly.gmail-mode}:
 *  - {@link MockGmailTool}  (default) seeds a fake inbox and simulates sending,
 *    so the whole system runs without Google credentials.
 *  - {@link RealGmailTool}  calls the actual Gmail REST API using stored OAuth
 *    tokens.
 */
public interface GmailTool {

    /** Fetch up to {@code max} recent messages from the user's inbox. */
    List<FetchedEmail> fetchRecent(String userEmail, int max);

    /**
     * Send a reply, preserving threading via the supplied thread id and headers.
     *
     * @return metadata about the message Gmail accepted.
     * @throws SendFailedException if delivery fails (e.g. expired token, quota).
     */
    SentMessage sendReply(OutboundMessage message) throws SendFailedException;

    /** A message fetched from the inbox. */
    record FetchedEmail(
            String gmailMessageId,
            String threadId,
            String sender,
            String recipient,
            String subject,
            String body,
            Instant receivedAt,
            boolean unread
    ) {
    }

    /**
     * A reply to be sent. {@code inReplyToMessageId} and {@code threadId} keep
     * the reply correctly threaded in the conversation.
     *
     * {@code injectTransientFailures} is demo-only metadata: the mock tool will
     * fail this many times before succeeding (to demonstrate automatic retries).
     * The real Gmail tool ignores it.
     */
    record OutboundMessage(
            String userEmail,
            String threadId,
            String inReplyToMessageId,
            String to,
            String subject,
            String body,
            int injectTransientFailures
    ) {
    }

    /** Metadata returned after a successful send. */
    record SentMessage(
            String sentGmailMessageId,
            String threadId
    ) {
    }
}
