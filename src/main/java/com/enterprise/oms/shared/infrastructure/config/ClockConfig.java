package com.enterprise.oms.shared.infrastructure.config;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single injectable {@link Clock}: UTC, ticking in microseconds. Every supported database stores at
 * least 6 fractional digits, so an {@link java.time.Instant} produced here survives a round-trip
 * unchanged on all vendors (JVM instants otherwise carry nanoseconds that the column silently truncates).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.of(1, ChronoUnit.MICROS));
    }
}
