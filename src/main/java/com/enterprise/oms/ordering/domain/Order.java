package com.enterprise.oms.ordering.domain;

import com.enterprise.oms.ordering.domain.events.OrderCancelledEvent;
import com.enterprise.oms.ordering.domain.events.OrderPlacedEvent;
import com.enterprise.oms.ordering.domain.events.OrderStatusChangedEvent;
import com.enterprise.oms.shared.domain.AggregateRoot;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.Money;
import com.enterprise.oms.shared.domain.UuidV7;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Order aggregate root. All invariants (non-empty, single currency, legal status transitions) are
 * enforced here, never in services or controllers. Table is {@code orders} because ORDER is reserved
 * on every SQL vendor.
 */
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {

    public static final String AGGREGATE_TYPE = "Order";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 32, unique = true)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // portable: plain varchar on every vendor (MySQL would expect ENUM otherwise)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber asc")
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false, precision = 19, scale = 4))
    @AttributeOverride(name = "currency", column = @Column(name = "total_currency", nullable = false, length = 3))
    private Money total;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    protected Order() {
    }

    private Order(UUID id, String orderNumber, UUID customerId, Instant placedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        this.placedAt = placedAt;
    }

    public static Order place(String orderNumber, UUID customerId, List<OrderLine> lines, Instant now) {
        Objects.requireNonNull(orderNumber, "orderNumber");
        Objects.requireNonNull(customerId, "customerId");
        if (lines == null || lines.isEmpty()) {
            throw new BusinessRuleViolationException("EMPTY_ORDER", "An order must contain at least one line");
        }
        Order order = new Order(UuidV7.generate(), orderNumber, customerId, now);
        int lineNumber = 1;
        for (OrderLine line : lines) {
            if (line.quantity() <= 0) {
                throw new BusinessRuleViolationException("INVALID_QUANTITY", "Quantity must be positive for " + line.sku());
            }
            order.items.add(new OrderItem(order, lineNumber++, line));
        }
        order.total = order.items.stream().map(OrderItem::getLineTotal)
                .reduce(Money.zero(order.items.getFirst().getLineTotal().currency()), Money::add);
        order.registerEvent(OrderPlacedEvent.of(order, now));
        return order;
    }

    public void confirm(Instant now) {
        transition(OrderStatus.CONFIRMED, now);
    }

    public void markPaid(Instant now) {
        transition(OrderStatus.PAID, now);
    }

    public void ship(Instant now) {
        transition(OrderStatus.SHIPPED, now);
    }

    public void deliver(Instant now) {
        transition(OrderStatus.DELIVERED, now);
    }

    public void cancel(String reason, Instant now) {
        transition(OrderStatus.CANCELLED, now);
        this.cancelReason = reason;
        registerEvent(OrderCancelledEvent.of(this, reason, now));
    }

    private void transition(OrderStatus next, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleViolationException("ILLEGAL_STATUS_TRANSITION",
                    "Order %s cannot go from %s to %s".formatted(orderNumber, status, next));
        }
        OrderStatus previous = status;
        status = next;
        registerEvent(OrderStatusChangedEvent.of(this, previous, next, now));
    }

    /** productId -> quantity, merged across lines. Used for stock reservation/release. */
    public Map<UUID, Integer> quantitiesByProduct() {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        for (OrderItem item : items) {
            result.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return result;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotal() {
        return total;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }
}
