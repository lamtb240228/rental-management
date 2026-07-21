package com.example.rental.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.rental.auth.entity.RefreshSession;
import com.example.rental.auth.repository.RefreshSessionRepository;
import com.example.rental.common.exception.UnauthorizedException;
import com.example.rental.common.security.RefreshTokenCodec;
import com.example.rental.common.security.RefreshTokenProperties;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.entity.UserStatus;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshSessionServiceTests {
    private static final String RAW_TOKEN = "presented-refresh-token";
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final long USER_ACCOUNT_ID = 42L;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RefreshTokenCodec refreshTokenCodec;

    @Mock
    private UserAccount userAccount;

    @Mock
    private RefreshSession refreshSession;

    private RefreshSessionService service;

    @BeforeEach
    void setUp() {
        service = new RefreshSessionService(
            refreshSessionRepository,
            userAccountRepository,
            refreshTokenCodec,
            new RefreshTokenProperties(7, 32, "rental_refresh", "/api/auth", false, "Strict")
        );
    }

    private void givenPresentedToken() {
        when(refreshTokenCodec.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshSessionRepository.findUserAccountIdByTokenHash(TOKEN_HASH))
            .thenReturn(Optional.of(USER_ACCOUNT_ID));
        when(userAccountRepository.findByIdForUpdate(USER_ACCOUNT_ID)).thenReturn(Optional.of(userAccount));
        when(refreshSessionRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(refreshSession));
        when(userAccount.getId()).thenReturn(USER_ACCOUNT_ID);
        when(refreshSession.getUserAccount()).thenReturn(userAccount);
    }

    @Test
    void familyRevocationLocksUserBeforeReloadingAndLockingSession() {
        givenPresentedToken();
        UUID familyId = UUID.randomUUID();
        when(refreshSession.getFamilyId()).thenReturn(familyId);

        service.revokeFamilyForToken(RAW_TOKEN);

        InOrder lockOrder = inOrder(refreshSessionRepository, userAccountRepository);
        lockOrder.verify(refreshSessionRepository).findUserAccountIdByTokenHash(TOKEN_HASH);
        lockOrder.verify(userAccountRepository).findByIdForUpdate(USER_ACCOUNT_ID);
        lockOrder.verify(refreshSessionRepository).findByTokenHashForUpdate(TOKEN_HASH);
        lockOrder.verify(refreshSessionRepository).revokeActiveFamily(eq(familyId), any());
    }

    @Test
    void logoutAllWithRotatedPredecessorRevokesOnlyItsFamilyAndRejectsRequest() {
        givenPresentedToken();
        UUID familyId = UUID.randomUUID();
        when(refreshSession.isRevoked()).thenReturn(true);
        when(refreshSession.wasRotated()).thenReturn(true);
        when(refreshSession.getFamilyId()).thenReturn(familyId);

        assertThatThrownBy(() -> service.revokeAllForToken(RAW_TOKEN))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Refresh session is invalid or expired");

        verify(refreshSessionRepository).revokeActiveFamily(
            eq(familyId),
            any()
        );
        verify(refreshSessionRepository, never()).revokeAllActiveForUser(
            anyLong(),
            any()
        );
        verify(userAccount, never()).invalidateAuthentication();
    }

    @Test
    void productionStatusLockAndGlobalRevocationUseTheLockedAccount() {
        when(userAccountRepository.findByIdForUpdate(USER_ACCOUNT_ID)).thenReturn(Optional.of(userAccount));
        when(userAccount.getId()).thenReturn(USER_ACCOUNT_ID);

        service.setUserStatusAndRevokeAll(USER_ACCOUNT_ID, UserStatus.LOCKED);

        InOrder securityOrder = inOrder(userAccountRepository, userAccount, refreshSessionRepository);
        securityOrder.verify(userAccountRepository).findByIdForUpdate(USER_ACCOUNT_ID);
        securityOrder.verify(userAccount).setStatus(UserStatus.LOCKED);
        securityOrder.verify(userAccount).invalidateAuthentication();
        securityOrder.verify(refreshSessionRepository).revokeAllActiveForUser(eq(USER_ACCOUNT_ID), any());
    }
}
