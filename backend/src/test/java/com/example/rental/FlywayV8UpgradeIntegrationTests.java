package com.example.rental;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FlywayV8UpgradeIntegrationTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void upgradesAnExistingV7SchemaWithoutReapplyingOlderMigrations() throws Exception {
        Flyway v7 = flyway(MigrationVersion.fromVersion("7"));
        v7.migrate();

        assertThat(v7.info().current().getVersion()).isEqualTo(MigrationVersion.fromVersion("7"));
        assertThat(columnExists("auth_version")).isFalse();

        Flyway current = flyway(null);
        current.migrate();

        assertThat(current.info().current().getVersion()).isEqualTo(MigrationVersion.fromVersion("8"));
        assertThat(successfulMigrationVersions())
            .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
        assertThat(authVersionDefinition()).isEqualTo("NO:0");
        assertThat(authVersionCheckExists()).isTrue();
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private boolean columnExists(String columnName) throws Exception {
        try (Connection connection = postgres.createConnection("");
             var statement = connection.prepareStatement("""
                 select exists (
                     select 1
                     from information_schema.columns
                     where table_schema = 'public'
                       and table_name = 'user_accounts'
                       and column_name = ?
                 )
                 """)) {
            statement.setString(1, columnName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private java.util.List<String> successfulMigrationVersions() throws Exception {
        try (Connection connection = postgres.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                 select version
                 from flyway_schema_history
                 where success = true
                 order by installed_rank
                 """)) {
            var versions = new java.util.ArrayList<String>();
            while (result.next()) {
                versions.add(result.getString(1));
            }
            return versions;
        }
    }

    private String authVersionDefinition() throws Exception {
        try (Connection connection = postgres.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                 select is_nullable || ':' || column_default
                 from information_schema.columns
                 where table_schema = 'public'
                   and table_name = 'user_accounts'
                   and column_name = 'auth_version'
                 """)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private boolean authVersionCheckExists() throws Exception {
        try (Connection connection = postgres.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                 select exists (
                     select 1
                     from pg_constraint
                     where conrelid = 'public.user_accounts'::regclass
                       and conname = 'ck_user_accounts_auth_version_nonnegative'
                       and contype = 'c'
                 )
                 """)) {
            result.next();
            return result.getBoolean(1);
        }
    }
}
