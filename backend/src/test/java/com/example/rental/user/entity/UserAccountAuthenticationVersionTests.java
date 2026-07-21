package com.example.rental.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAccountAuthenticationVersionTests {
    @Test
    void softDeleteInvalidatesAuthenticationExactlyOnce() {
        UserAccount userAccount = new UserAccount();

        userAccount.softDelete();
        userAccount.softDelete();

        assertThat(userAccount.getDeletedAt()).isNotNull();
        assertThat(userAccount.getAuthVersion()).isEqualTo(1);
    }
}
