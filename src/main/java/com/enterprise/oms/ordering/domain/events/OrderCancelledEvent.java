package com.enterprise.oms.ordering.domain.events;

import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.shared.domain.DomainEvent;
import com.enterprise.oms.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderNumber,
        String reason) implements DomainEvent {

    public static OrderCancelledEvent of(Order order, String reason, Instant now) {
        return new OrderCancelledEvent(UuidV7.generate(), now, order.getId(), order.getOrderNumber(), reason);
    }

    @Override
    public String aggregateType() {
        return Order.AGGREGATE_TYPE;
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
