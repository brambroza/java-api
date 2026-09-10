package com.enterprise.oms.ordering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterprise.oms.ordering.domain.events.OrderCancelledEvent;
import com.enterprise.oms.ordering.domain.events.OrderPlacedEvent;
import com.enterprise.oms.ordering.domain.events.OrderStatusChangedEvent;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
    private static final UUID CUSTOMER = UUID.randomUUID();

    private static OrderLine line(String sku, int qty, String price) {
        return new OrderLine(UUID.randomUUID(), sku, "Product " + sku, qty, Money.of(price, "THB"));
    }

    @Test
    void placingAnOrderComputesTotalsAndRegistersEvent() {
        Order order = Order.place("ORD-1", CUSTOMER, List.of(line("A", 2, "100"), line("B", 1, "50.5")), NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getLineNumber()).isEqualTo(1);
        assertThat(order.getItems().get(0).getLineTotal()).isEqualTo(Money.of("200", "THB"));
        assertThat(order.getTotal()).isEqualTo(Money.of("250.5", "THB"));
        assertThat(order.domainEvents()).singleElement().isInstanceOf(OrderPlacedEvent.class);
    }

    @Test
    void rejectsEmptyOrders() {
        assertThatThrownBy(() -> Order.place("ORD-1", CUSTOMER, List.of(), NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("EMPTY_ORDER");
    }

    @Test
    void rejectsMixedCurrencies() {
        OrderLine eur = new OrderLine(UUID.randomUUID(), "E", "Euro item", 1, Money.of("1", "EUR"));
        assertThatThrownBy(() -> Order.place("ORD-1", CUSTOMER, List.of(line("A", 1, "1"), eur), NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("CURRENCY_MISMATCH");
    }

    @Test
    void followsTheStatusStateMachine() {
        Order order = Order.place("ORD-1", CUSTOMER, List.of(line("A", 1, "10")), NOW);
        order.clearDomainEvents();

        order.confirm(NOW);
        order.markPaid(NOW);
        order.ship(NOW);
        order.deliver(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.domainEvents()).hasSize(4).allMatch(OrderStatusChangedEvent.class::isInstance);
        assertThatThrownBy(() -> order.cancel("too late", NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("ILLEGAL_STATUS_TRANSITION");
    }

    @Test
    void cancellationRecordsReasonAndEvent() {
        Order order = Order.place("ORD-1", CUSTOMER, List.of(line("A", 1, "10")), NOW);
        order.clearDomainEvents();

        order.cancel("customer changed mind", NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("customer changed mind");
        assertThat(order.domainEvents()).hasSize(2);
        assertThat(order.domainEvents().stream().filter(OrderCancelledEvent.class::isInstance)).hasSize(1);
    }

    @Test
    void mergesQuantitiesPerProduct() {
        UUID product = UUID.randomUUID();
        OrderLine first = new OrderLine(product, "A", "A", 2, Money.of("1", "THB"));
        OrderLine second = new OrderLine(product, "A", "A", 3, Money.of("1", "THB"));
        Order order = Order.place("ORD-1", CUSTOMER, List.of(first, second), NOW);
        assertThat(order.quantitiesByProduct()).containsEntry(product, 5);
    }
}
