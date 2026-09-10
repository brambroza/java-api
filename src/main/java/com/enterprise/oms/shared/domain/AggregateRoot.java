package com.enterprise.oms.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

/**
 * Base class for aggregate roots: optimistic locking + domain event collection.
 * <p>
 * {@code version} is a boxed {@link Long} on purpose: Spring Data treats a {@code null} version as
 * "new entity" and calls {@code persist} directly instead of {@code merge}, which saves one SELECT
 * per insert when identifiers are assigned in the application (UUID v7).
 */
@MappedSuperclass
public abstract class AggregateRoot extends AuditableEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    public Long getVersion() {
        return version;
    }

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    @DomainEvents
    public Collection<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
