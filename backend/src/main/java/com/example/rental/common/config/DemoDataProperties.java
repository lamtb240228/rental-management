package com.example.rental.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo-data")
public record DemoDataProperties(boolean enabled) {
    public static final List<String> ACCOUNT_EMAILS = List.of(
        "demo@rental.local",
        "admin@rental.local",
        "tenant@rental.local"
    );
}
