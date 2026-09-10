package com.enterprise.oms.shared.infrastructure.outbox;

import com.enterprise.oms.shared.application.EventPublisher;
import com.enterprise.oms.shared.application.OutboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default adapter: logs events. Replace with a Kafka/SQS adapter by setting {@code app.outbox.publisher}. */
@Component
@ConditionalOnProperty(name = "app.outbox.publisher", havingValue = "log", matchIfMissing = true)
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(OutboxMessage message) {
        log.info("event published type={} aggregate={}:{} id={} payload={}",
                message.eventType(), message.aggregateType(), message.aggregateId(), message.id(), message.payload());
    }
}
