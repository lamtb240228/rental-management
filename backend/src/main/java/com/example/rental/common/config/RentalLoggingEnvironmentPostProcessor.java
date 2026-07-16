package com.example.rental.common.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

/**
 * Prevents generic operating-system variables such as DEBUG or TRACE from
 * enabling Spring's verbose request logging by accident. Debug logging must be
 * requested explicitly with a project-scoped variable and is always disabled
 * for the production profile.
 */
public class RentalLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    static final String PROPERTY_SOURCE_NAME = "rentalLoggingGuard";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        boolean debug = !production && Boolean.parseBoolean(environment.getProperty("RENTAL_DEBUG", "false"));
        boolean trace = !production && Boolean.parseBoolean(environment.getProperty("RENTAL_TRACE", "false"));

        environment.getPropertySources().addFirst(new MapPropertySource(
            PROPERTY_SOURCE_NAME,
            Map.of("debug", debug, "trace", trace)
        ));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
