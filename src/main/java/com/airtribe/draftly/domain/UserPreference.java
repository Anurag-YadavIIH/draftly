package com.airtribe.draftly.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Per-user preferences that influence draft generation: the signature appended
 * to replies and the default tone to use when the caller does not specify one.
 */
@Entity
@Table(name = "user_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_email"))
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(columnDefinition = "TEXT")
    private String signature;

    private String defaultTone = "formal";

    private Instant updatedAt = Instant.now();

    public UserPreference() {
    }

    public UserPreference(String userEmail, String signature, String defaultTone) {
        this.userEmail = userEmail;
        this.signature = signature;
        this.defaultTone = defaultTone;
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getDefaultTone() { return defaultTone; }
    public void setDefaultTone(String defaultTone) { this.defaultTone = defaultTone; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
