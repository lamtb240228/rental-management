package com.example.rental.auth.service;

import com.example.rental.auth.entity.RefreshSession;
import com.example.rental.auth.repository.RefreshSessionRepository;
import com.example.rental.common.exception.UnauthorizedException;
import com.example.rental.common.security.RefreshTokenCodec;
import com.example.rental.common.security.RefreshTokenCodec.RefreshTokenValue;
import com.example.rental.common.security.RefreshTokenProperties;
import com.example.rental.user.entity.UserAccount;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshSessionService {
    private static final String INVALID_REFRESH_SESSION = "Refresh session is invalid or expired";

    private final RefreshSessionRepository repository;
    private final RefreshTokenCodec tokenCodec;
    private final RefreshTokenProperties properties;

    public RefreshSessionService(
        RefreshSessionRepository repository,
        RefreshTokenCodec tokenCodec,
        RefreshTokenProperties properties
    ) {
        this.repository = repository;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
    }

    @Transactional
    public RefreshSessionGrant issue(UserAccount userAccount) {
        if (userAccount == null || !userAccount.isActive()) {
            throw invalidSession();
        }
        OffsetDateTime createdAt = now();
        OffsetDateTime expiresAt = createdAt.plusDays(properties.expirationDays());
        return persistGrant(userAccount, UUID.randomUUID(), createdAt, expiresAt);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RefreshSessionGrant rotate(String rawToken) {
        RefreshSession current = findForUpdate(rawToken);
        OffsetDateTime usedAt = now();

        if (current.isRevoked()) {
            if (current.wasRotated()) {
                repository.revokeActiveFamily(current.getFamilyId(), usedAt);
            }
            throw invalidSession();
        }
        if (current.isExpiredAt(usedAt)) {
            repository.revokeActiveFamily(current.getFamilyId(), usedAt);
            throw invalidSession();
        }

        UserAccount userAccount = current.getUserAccount();
        if (!userAccount.isActive()) {
            repository.revokeAllActiveForUser(userAccount.getId(), usedAt);
            throw invalidSession();
        }

        RefreshTokenValue token = tokenCodec.generate();
        RefreshSession replacement = RefreshSession.issue(
            userAccount,
            token.tokenHash(),
            current.getFamilyId(),
            usedAt,
            current.getExpiresAt()
        );
        repository.saveAndFlush(replacement);
        current.rotateTo(replacement, usedAt);
        return new RefreshSessionGrant(token.rawToken(), current.getExpiresAt(), userAccount);
    }

    @Transactional
    public void revokeFamilyForToken(String rawToken) {
        findOptionalForUpdate(rawToken).ifPresent(session ->
            repository.revokeActiveFamily(session.getFamilyId(), now())
        );
    }

    @Transactional
    public void revokeAllForToken(String rawToken) {
        findOptionalForUpdate(rawToken).ifPresent(session ->
            repository.revokeAllActiveForUser(session.getUserAccount().getId(), now())
        );
    }

    @Transactional
    public void revokeAllForUser(Long userAccountId) {
        if (userAccountId != null) {
            repository.revokeAllActiveForUser(userAccountId, now());
        }
    }

    private RefreshSessionGrant persistGrant(
        UserAccount userAccount,
        UUID familyId,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
    ) {
        RefreshTokenValue token = tokenCodec.generate();
        RefreshSession session = RefreshSession.issue(
            userAccount,
            token.tokenHash(),
            familyId,
            createdAt,
            expiresAt
        );
        repository.saveAndFlush(session);
        return new RefreshSessionGrant(token.rawToken(), expiresAt, userAccount);
    }

    private RefreshSession findForUpdate(String rawToken) {
        return findOptionalForUpdate(rawToken).orElseThrow(this::invalidSession);
    }

    private Optional<RefreshSession> findOptionalForUpdate(String rawToken) {
        try {
            return repository.findByTokenHashForUpdate(tokenCodec.hash(rawToken));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private UnauthorizedException invalidSession() {
        return new UnauthorizedException(INVALID_REFRESH_SESSION);
    }
}
