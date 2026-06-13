package com.airtribe.draftly.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A past email the user actually sent, used as RAG context so generated drafts
 * match the user's tone, phrasing and style. The {@code embedding} is stored as
 * a comma-separated list of doubles; at retrieval time we score each sample by
 * cosine similarity against the current email and pick the closest few.
 */
@Entity
@Table(name = "style_sample")
public class StyleSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(columnDefinition = "TEXT")
    private String text;

    /** Comma-separated vector, e.g. "0.12,0.04,-0.91,...". */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    private Instant createdAt = Instant.now();

    public StyleSample() {
    }

    public StyleSample(String userEmail, String text, String embedding) {
        this.userEmail = userEmail;
        this.text = text;
        this.embedding = embedding;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
