package com.enterprise.oms.ordering.domain.events;

import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.ordering.domain.OrderStatus;
import com.enterprise.oms.shared.domain.DomainEvent;
import com.enterprise.oms.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus newStatus) implements DomainEvent {

    public static OrderStatusChangedEvent of(Order order, OrderStatus previous, OrderStatus next, Instant now) {
        return new OrderStatusChangedEvent(UuidV7.generate(), now, order.getId(), order.getOrderNumber(), previous, next);
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
