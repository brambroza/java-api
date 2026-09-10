package com.enterprise.oms.shared.application;

/**
 * Outbound port for integration events. The outbox relay calls it for each pending row; provide a
 * Kafka / SQS / Pub-Sub adapter by implementing this interface and setting {@code app.outbox.publisher}.
 */
public interface EventPublisher {

    void publish(OutboxMessage message);
}
