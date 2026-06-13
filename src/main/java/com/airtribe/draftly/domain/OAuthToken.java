package com.airtribe.draftly.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores a user's Gmail OAuth2 tokens. Both the access and refresh tokens are
 * encrypted with AES before being written here (see EncryptionService), so the
 * database never holds tokens in plain text.
 */
@Entity
@Table(name = "oauth_token",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_email"))
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(columnDefinition = "TEXT")
    private String accessTokenEnc;

    @Column(columnDefinition = "TEXT")
    private String refreshTokenEnc;

    private Instant expiresAt;

    private String scope;

    private Instant updatedAt = Instant.now();

    public OAuthToken() {
    }

    public OAuthToken(String userEmail, String accessTokenEnc, String refreshTokenEnc,
                      Instant expiresAt, String scope) {
        this.userEmail = userEmail;
        this.accessTokenEnc = accessTokenEnc;
        this.refreshTokenEnc = refreshTokenEnc;
        this.expiresAt = expiresAt;
        this.scope = scope;
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getAccessTokenEnc() { return accessTokenEnc; }
    public void setAccessTokenEnc(String accessTokenEnc) { this.accessTokenEnc = accessTokenEnc; }
    public String getRefreshTokenEnc() { return refreshTokenEnc; }
    public void setRefreshTokenEnc(String refreshTokenEnc) { this.refreshTokenEnc = refreshTokenEnc; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
