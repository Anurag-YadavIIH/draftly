package com.airtribe.draftly.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Audit record for every send attempt. The {@code idempotencyKey} carries a
 * UNIQUE constraint so that retrying the same logical send can never create a
 * duplicate outgoing email - this is how we make sending idempotent.
 */
@Entity
@Table(name = "sent_log",
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class SentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftId;

    private String threadId;

    /** Gmail message id returned after a successful send. */
    private String sentGmailMessageId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status;

    private int attempts = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private Instant createdAt = Instant.now();

    private Instant sentAt;

    public SentLog() {
    }

    public SentLog(Long draftId, String threadId, String idempotencyKey) {
        this.draftId = draftId;
        this.threadId = threadId;
        this.idempotencyKey = idempotencyKey;
        this.status = DraftStatus.APPROVED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDraftId() { return draftId; }
    public void setDraftId(Long draftId) { this.draftId = draftId; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getSentGmailMessageId() { return sentGmailMessageId; }
    public void setSentGmailMessageId(String sentGmailMessageId) { this.sentGmailMessageId = sentGmailMessageId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public DraftStatus getStatus() { return status; }
    public void setStatus(DraftStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
