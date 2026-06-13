package com.airtribe.draftly.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * An email fetched from the user's Gmail inbox. We persist the metadata Gmail
 * gives us (sender, subject, thread id, etc.) so drafts can be generated and
 * correct threading can be preserved when replying.
 */
@Entity
@Table(name = "email_message",
        uniqueConstraints = @UniqueConstraint(columnNames = "gmail_message_id"))
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this inbox (single-user demo uses a constant). */
    @Column(nullable = false)
    private String userEmail;

    /** Gmail's own message id. Unique so the same email is never imported twice. */
    @Column(name = "gmail_message_id", nullable = false)
    private String gmailMessageId;

    /** Gmail thread id - needed to keep replies in the same conversation. */
    @Column(nullable = false)
    private String threadId;

    @Column(nullable = false)
    private String sender;

    private String recipient;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Instant receivedAt;

    private boolean unread = true;

    public EmailMessage() {
    }

    public EmailMessage(String userEmail, String gmailMessageId, String threadId, String sender,
                        String recipient, String subject, String body, Instant receivedAt, boolean unread) {
        this.userEmail = userEmail;
        this.gmailMessageId = gmailMessageId;
        this.threadId = threadId;
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.receivedAt = receivedAt;
        this.unread = unread;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getGmailMessageId() { return gmailMessageId; }
    public void setGmailMessageId(String gmailMessageId) { this.gmailMessageId = gmailMessageId; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }
}
