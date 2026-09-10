package com.enterprise.oms.shared.application;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Instant occurredAt) {
}
