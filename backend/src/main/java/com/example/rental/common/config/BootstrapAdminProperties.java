package com.example.rental.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password, String fullName) {
    @Override
    public String toString() {
        return "BootstrapAdminProperties[email=" + email
            + ", password=[REDACTED], fullName=" + fullName + "]";
    }
}
