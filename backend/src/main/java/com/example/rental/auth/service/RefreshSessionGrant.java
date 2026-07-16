package com.example.rental.auth.service;

import com.example.rental.user.entity.UserAccount;
import java.time.OffsetDateTime;

public final class RefreshSessionGrant {
    private final String rawToken;
    private final OffsetDateTime expiresAt;
    private final UserAccount userAccount;

    RefreshSessionGrant(String rawToken, OffsetDateTime expiresAt, UserAccount userAccount) {
        this.rawToken = rawToken;
        this.expiresAt = expiresAt;
        this.userAccount = userAccount;
    }

    public String rawToken() {
        return rawToken;
    }

    public OffsetDateTime expiresAt() {
        return expiresAt;
    }

    public UserAccount userAccount() {
        return userAccount;
    }

    @Override
    public String toString() {
        return "RefreshSessionGrant[rawToken=[REDACTED], expiresAt=" + expiresAt
            + ", userAccountId=" + userAccount.getId() + "]";
    }
}
