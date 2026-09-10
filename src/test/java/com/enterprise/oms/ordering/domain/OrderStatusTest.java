package com.enterprise.oms.ordering.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void terminalStatesHaveNoTransitions() {
        assertThat(OrderStatus.DELIVERED.allowedTransitions()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedTransitions()).isEmpty();
    }

    @Test
    void reservationIsHeldUntilShipped() {
        assertThat(OrderStatus.PENDING.holdsReservation()).isTrue();
        assertThat(OrderStatus.PAID.holdsReservation()).isTrue();
        assertThat(OrderStatus.SHIPPED.holdsReservation()).isFalse();
        assertThat(OrderStatus.CANCELLED.holdsReservation()).isFalse();
    }

    @Test
    void cannotSkipSteps() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
    }
}
