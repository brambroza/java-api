package com.enterprise.oms.ordering.domain.events;

import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.ordering.domain.OrderItem;
import com.enterprise.oms.shared.domain.DomainEvent;
import com.enterprise.oms.shared.domain.Money;
import com.enterprise.oms.shared.domain.UuidV7;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderNumber,
        UUID customerId,
        Money total,
        List<Line> lines) implements DomainEvent {

    public record Line(UUID productId, String sku, int quantity, Money unitPrice) {
    }

    public static OrderPlacedEvent of(Order order, Instant now) {
        return new OrderPlacedEvent(UuidV7.generate(), now, order.getId(), order.getOrderNumber(), order.getCustomerId(),
                order.getTotal(), order.getItems().stream().map(OrderPlacedEvent::line).toList());
    }

    private static Line line(OrderItem item) {
        return new Line(item.getProductId(), item.getSku(), item.getQuantity(), item.getUnitPrice());
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
