package com.enterprise.oms.shared.infrastructure.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Pending rows, oldest first, locked with SKIP LOCKED where the vendor supports it
     * (PostgreSQL, MySQL 8, MariaDB, SQL Server READPAST, Oracle) so several relay instances can run
     * concurrently. Hibernate falls back to a plain FOR UPDATE on vendors without SKIP LOCKED (H2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEvent> findByPublishedAtIsNullOrderByOccurredAtAsc(Limit limit);

    long countByPublishedAtIsNull();

    List<OutboxEvent> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);
}
