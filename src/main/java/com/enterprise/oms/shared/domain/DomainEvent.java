package com.enterprise.oms.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened inside an aggregate. Events are recorded in the transactional outbox in
 * the same transaction as the aggregate change and published asynchronously afterwards.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String aggregateType();

    String aggregateId();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
