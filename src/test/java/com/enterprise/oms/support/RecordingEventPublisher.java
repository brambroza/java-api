package com.enterprise.oms.support;

import com.enterprise.oms.shared.application.EventPublisher;
import com.enterprise.oms.shared.application.OutboxMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Test adapter for the outbox relay: captures published messages in memory. */
@Component
@ConditionalOnProperty(name = "app.outbox.publisher", havingValue = "recording")
public class RecordingEventPublisher implements EventPublisher {

    private final List<OutboxMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public void publish(OutboxMessage message) {
        messages.add(message);
    }

    public List<OutboxMessage> messages() {
        return List.copyOf(messages);
    }

    public void clear() {
        messages.clear();
    }
}
