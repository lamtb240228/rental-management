package com.example.rental.common.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.rental.auth.entity.Role;
import com.example.rental.auth.entity.RoleName;
import com.example.rental.auth.repository.RoleRepository;
import com.example.rental.auth.service.RefreshSessionService;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.entity.UserStatus;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DemoDataConfigurationTests {
    @Mock
    private UserAccountRepository repository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshSessionService refreshSessionService;

    private final DemoDataConfiguration configuration = new DemoDataConfiguration();

    @Test
    void productionStartupLocksAnActiveKnownDemoAccount() throws Exception {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(100L);
        when(repository.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString()))
            .thenReturn(Optional.of(account));
        when(repository.existsDistinctByRolesNameAndStatusAndDeletedAtIsNull(RoleName.ADMIN, UserStatus.ACTIVE))
            .thenReturn(true);

        configuration.secureProductionAccounts(
            repository,
            roleRepository,
            passwordEncoder,
            new BootstrapAdminProperties(null, null, null),
            refreshSessionService
        ).run(null);

        verify(refreshSessionService, times(DemoDataProperties.ACCOUNT_EMAILS.size()))
            .setUserStatusAndRevokeAll(100L, UserStatus.LOCKED);
    }

    @Test
    void productionStartupAcceptsLockedDemoAccounts() {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(100L);
        when(repository.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString()))
            .thenReturn(Optional.of(account));
        when(repository.existsDistinctByRolesNameAndStatusAndDeletedAtIsNull(RoleName.ADMIN, UserStatus.ACTIVE))
            .thenReturn(true);

        assertDoesNotThrow(
            () -> configuration.secureProductionAccounts(
                repository,
                roleRepository,
                passwordEncoder,
                new BootstrapAdminProperties(null, null, null),
                refreshSessionService
            ).run(null)
        );
        verify(refreshSessionService, times(DemoDataProperties.ACCOUNT_EMAILS.size()))
            .setUserStatusAndRevokeAll(100L, UserStatus.LOCKED);
    }

    @Test
    void productionStartupFailsClosedWithoutAnActiveOrBootstrapAdministrator() {
        assertThrows(
            IllegalStateException.class,
            () -> configuration.secureProductionAccounts(
                repository,
                roleRepository,
                passwordEncoder,
                new BootstrapAdminProperties(null, null, null),
                refreshSessionService
            ).run(null)
        );
    }

    @Test
    void productionStartupCreatesExplicitStrongBootstrapAdministrator() throws Exception {
        Role adminRole = new Role();
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("StrongBootstrap123!")) .thenReturn("encoded-password");

        configuration.secureProductionAccounts(
            repository,
            roleRepository,
            passwordEncoder,
            new BootstrapAdminProperties(
                "  First.Admin@Example.Invalid  ",
                "StrongBootstrap123!",
                "First Administrator"
            ),
            refreshSessionService
        ).run(null);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).saveAndFlush(accountCaptor.capture());
        UserAccount administrator = accountCaptor.getValue();
        assertEquals("first.admin@example.invalid", administrator.getEmail());
        assertEquals("encoded-password", administrator.getPasswordHash());
        assertTrue(administrator.isEmailVerified());
        assertTrue(administrator.getRoles().contains(adminRole));
    }
}
