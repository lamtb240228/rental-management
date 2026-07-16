package com.example.rental.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RentalLoggingEnvironmentPostProcessorTests {
    private final RentalLoggingEnvironmentPostProcessor postProcessor =
        new RentalLoggingEnvironmentPostProcessor();

    @Test
    void genericDebugAndTracePropertiesAreIgnored() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("debug", "true")
            .withProperty("trace", "true");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("debug", Boolean.class)).isFalse();
        assertThat(environment.getProperty("trace", Boolean.class)).isFalse();
    }

    @Test
    void projectScopedDebugCanBeEnabledOutsideProduction() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("RENTAL_DEBUG", "true");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("debug", Boolean.class)).isTrue();
    }

    @Test
    void productionAlwaysDisablesVerboseBootstrapLogging() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("RENTAL_DEBUG", "true")
            .withProperty("RENTAL_TRACE", "true");
        environment.setActiveProfiles("prod");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("debug", Boolean.class)).isFalse();
        assertThat(environment.getProperty("trace", Boolean.class)).isFalse();
    }
}
