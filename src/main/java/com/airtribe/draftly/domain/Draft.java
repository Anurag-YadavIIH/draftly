package com.airtribe.draftly.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * An AI-generated reply draft for a specific inbound email. A draft moves
 * through the {@link DraftStatus} lifecycle as the user reviews it.
 */
@Entity
@Table(name = "draft")
public class Draft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The inbound email this draft replies to. */
    @Column(nullable = false)
    private Long emailId;

    /** Denormalised for convenient threading when sending. */
    @Column(nullable = false)
    private String threadId;

    @Column(nullable = false)
    private String userEmail;

    /** formal | friendly | concise (free text, validated at the API layer). */
    private String tone;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.SUGGESTED;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Draft() {
    }

    public Draft(Long emailId, String threadId, String userEmail, String tone, String content) {
        this.emailId = emailId;
        this.threadId = threadId;
        this.userEmail = userEmail;
        this.tone = tone;
        this.content = content;
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmailId() { return emailId; }
    public void setEmailId(Long emailId) { this.emailId = emailId; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public DraftStatus getStatus() { return status; }
    public void setStatus(DraftStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
