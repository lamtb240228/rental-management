package com.example.rental.auth.service;

import com.example.rental.auth.entity.RefreshSession;
import com.example.rental.auth.repository.RefreshSessionRepository;
import com.example.rental.common.exception.UnauthorizedException;
import com.example.rental.common.security.RefreshTokenCodec;
import com.example.rental.common.security.RefreshTokenCodec.RefreshTokenValue;
import com.example.rental.common.security.RefreshTokenProperties;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.entity.UserStatus;
import com.example.rental.user.repository.UserAccountRepository;
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
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenCodec tokenCodec;
    private final RefreshTokenProperties properties;

    public RefreshSessionService(
        RefreshSessionRepository repository,
        UserAccountRepository userAccountRepository,
        RefreshTokenCodec tokenCodec,
        RefreshTokenProperties properties
    ) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
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
        LockedRefreshSession lockedSession = lockUserThenSession(rawToken).orElseThrow(this::invalidSession);
        RefreshSession current = lockedSession.session();
        UserAccount userAccount = lockedSession.userAccount();
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

        if (!userAccount.isActive()) {
            invalidateAndRevokeAll(userAccount, usedAt);
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
        lockUserThenSession(rawToken).ifPresent(lockedSession ->
            repository.revokeActiveFamily(lockedSession.session().getFamilyId(), now())
        );
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public void revokeAllForToken(String rawToken) {
        LockedRefreshSession lockedSession = lockUserThenSession(rawToken).orElseThrow(this::invalidSession);
        RefreshSession session = lockedSession.session();
        UserAccount userAccount = lockedSession.userAccount();
        OffsetDateTime revokedAt = now();
        if (session.isRevoked()) {
            if (session.wasRotated()) {
                repository.revokeActiveFamily(session.getFamilyId(), revokedAt);
            }
            throw invalidSession();
        }
        if (session.isExpiredAt(revokedAt)) {
            repository.revokeActiveFamily(session.getFamilyId(), revokedAt);
            throw invalidSession();
        }
        if (!userAccount.isActive()) {
            throw invalidSession();
        }
        invalidateAndRevokeAll(userAccount, revokedAt);
    }

    @Transactional
    public void revokeAllForUser(Long userAccountId) {
        if (userAccountId != null) {
            userAccountRepository.findByIdForUpdate(userAccountId)
                .ifPresent(userAccount -> invalidateAndRevokeAll(userAccount, now()));
        }
    }

    @Transactional
    public void setUserStatusAndRevokeAll(Long userAccountId, UserStatus status) {
        if (userAccountId == null || status == null || status == UserStatus.ACTIVE) {
            throw new IllegalArgumentException("A non-active account status is required");
        }
        userAccountRepository.findByIdForUpdate(userAccountId).ifPresent(userAccount -> {
            userAccount.setStatus(status);
            invalidateAndRevokeAll(userAccount, now());
        });
    }

    public Optional<Long> resolveUserAccountId(String rawToken) {
        return resolvePresentedToken(rawToken).map(PresentedToken::userAccountId);
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

    private Optional<LockedRefreshSession> lockUserThenSession(String rawToken) {
        Optional<PresentedToken> presentedToken = resolvePresentedToken(rawToken);
        if (presentedToken.isEmpty()) {
            return Optional.empty();
        }

        PresentedToken token = presentedToken.get();
        Optional<UserAccount> userAccount = userAccountRepository.findByIdForUpdate(token.userAccountId());
        if (userAccount.isEmpty()) {
            return Optional.empty();
        }

        return repository.findByTokenHashForUpdate(token.tokenHash())
            .filter(session -> session.getUserAccount().getId().equals(token.userAccountId()))
            .map(session -> new LockedRefreshSession(userAccount.get(), session));
    }

    private Optional<PresentedToken> resolvePresentedToken(String rawToken) {
        try {
            String tokenHash = tokenCodec.hash(rawToken);
            return repository.findUserAccountIdByTokenHash(tokenHash)
                .map(userAccountId -> new PresentedToken(tokenHash, userAccountId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void invalidateAndRevokeAll(UserAccount userAccount, OffsetDateTime revokedAt) {
        userAccount.invalidateAuthentication();
        repository.revokeAllActiveForUser(userAccount.getId(), revokedAt);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private UnauthorizedException invalidSession() {
        return new UnauthorizedException(INVALID_REFRESH_SESSION);
    }

    private record PresentedToken(String tokenHash, Long userAccountId) {
    }

    private record LockedRefreshSession(UserAccount userAccount, RefreshSession session) {
    }
}
