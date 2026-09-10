package com.enterprise.oms.shared.infrastructure.outbox;

import com.enterprise.oms.shared.domain.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

/**
 * Transactional outbox, write side. Aggregates register {@link DomainEvent}s; Spring Data publishes them
 * on {@code repository.save(...)}; this listener persists them <em>before commit of the same transaction</em>,
 * so an event exists if and only if the business change was committed.
 */
@Component
public class DomainEventOutboxRecorder {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public DomainEventOutboxRecorder(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void record(DomainEvent event) {
        repository.save(new OutboxEvent(
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                objectMapper.writeValueAsString(event),
                event.occurredAt()));
    }
}
