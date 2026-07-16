package com.example.rental.auth.entity;

import com.example.rental.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_session_id")
    private RefreshSession replacedBy;

    protected RefreshSession() {
    }

    private RefreshSession(
        UserAccount userAccount,
        String tokenHash,
        UUID familyId,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
    ) {
        this.userAccount = userAccount;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static RefreshSession issue(
        UserAccount userAccount,
        String tokenHash,
        UUID familyId,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
    ) {
        return new RefreshSession(userAccount, tokenHash, familyId, createdAt, expiresAt);
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public RefreshSession getReplacedBy() {
        return replacedBy;
    }

    public boolean isExpiredAt(OffsetDateTime instant) {
        return !expiresAt.isAfter(instant);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean wasRotated() {
        return replacedBy != null;
    }

    public void rotateTo(RefreshSession replacement, OffsetDateTime usedAt) {
        if (isRevoked()) {
            throw new IllegalStateException("Refresh session has already been revoked");
        }
        if (!familyId.equals(replacement.familyId)) {
            throw new IllegalArgumentException("Replacement must belong to the same refresh-session family");
        }
        lastUsedAt = usedAt;
        revokedAt = usedAt;
        replacedBy = replacement;
    }

    public void revoke(OffsetDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }
}
