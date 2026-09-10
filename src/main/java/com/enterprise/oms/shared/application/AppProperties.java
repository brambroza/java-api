package com.enterprise.oms.shared.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Type-safe, validated application settings (prefix {@code app}). */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Security security,
        @DefaultValue Scheduling scheduling,
        @DefaultValue Outbox outbox,
        @DefaultValue Idempotency idempotency,
        @DefaultValue("false") boolean seedData) {

    public record Security(@DefaultValue("true") boolean enabled, @DefaultValue Jwt jwt) {
        public record Jwt(String secret) {
        }
    }

    public record Scheduling(@DefaultValue("true") boolean enabled) {
    }

    public record Outbox(
            @DefaultValue("log") String publisher,
            @Min(1) @DefaultValue("50") int batchSize,
            @NotNull @DefaultValue("PT5S") Duration pollInterval) {
    }

    public record Idempotency(@NotNull @DefaultValue("PT24H") Duration ttl) {
    }
}
