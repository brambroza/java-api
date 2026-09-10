package com.enterprise.oms.shared.infrastructure.outbox;

import com.enterprise.oms.shared.application.EventPublisher;
import com.enterprise.oms.shared.application.OutboxMessage;
import com.enterprise.oms.shared.application.AppProperties;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Transactional outbox, read side: polls pending events and hands them to the {@link EventPublisher}. */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final EventPublisher publisher;
    private final AppProperties properties;
    private final Clock clock;

    public OutboxRelay(OutboxEventRepository repository, EventPublisher publisher, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    /** Returns the number of events published in this batch. */
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:PT5S}", initialDelayString = "PT10S")
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = repository.findByPublishedAtIsNullOrderByOccurredAtAsc(
                Limit.of(properties.outbox().batchSize()));
        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                publisher.publish(new OutboxMessage(event.getId(), event.getAggregateType(), event.getAggregateId(),
                        event.getEventType(), event.getPayload(), event.getOccurredAt()));
                event.markPublished(clock.instant());
                published++;
            } catch (RuntimeException failure) {
                event.recordFailure(failure.toString());
                log.warn("Failed to publish outbox event {} ({}), attempt {}: {}",
                        event.getId(), event.getEventType(), event.getAttempts(), failure.toString());
            }
        }
        if (!batch.isEmpty()) {
            log.debug("Outbox relay published {}/{} events", published, batch.size());
        }
        return published;
    }
}
