package com.example.rental.common.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabasePrivilegeConfiguration {
    @Bean
    @Profile("prod")
    ApplicationRunner verifyRuntimeDatabaseRole(JdbcTemplate jdbcTemplate) {
        return arguments -> {
            Boolean elevated = jdbcTemplate.queryForObject("""
                select rolsuper or rolcreatedb or rolcreaterole or rolreplication or rolbypassrls
                from pg_roles
                where rolname = current_user
                """, Boolean.class);
            if (elevated == null || elevated) {
                throw new IllegalStateException(
                    "Production datasource role must not have superuser, create-role, create-db, replication or bypass-RLS privileges"
                );
            }
        };
    }
}
