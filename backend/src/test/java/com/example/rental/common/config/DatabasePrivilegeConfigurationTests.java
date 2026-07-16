package com.example.rental.common.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DatabasePrivilegeConfigurationTests {
    @Mock
    private JdbcTemplate jdbcTemplate;

    private final DatabasePrivilegeConfiguration configuration = new DatabasePrivilegeConfiguration();

    @Test
    void productionAcceptsRestrictedRuntimeRole() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Boolean.class)))
            .thenReturn(false);

        assertDoesNotThrow(() -> configuration.verifyRuntimeDatabaseRole(jdbcTemplate).run(null));
    }

    @Test
    void productionRejectsElevatedRuntimeRole() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Boolean.class)))
            .thenReturn(true);

        assertThrows(
            IllegalStateException.class,
            () -> configuration.verifyRuntimeDatabaseRole(jdbcTemplate).run(null)
        );
    }
}
