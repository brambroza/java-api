package com.enterprise.oms.ordering.domain;

import java.util.EnumSet;
import java.util.Set;

/** Order lifecycle. Transitions are the single source of truth for the state machine. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }

    /** While true, the order holds a stock reservation that must be released on cancellation. */
    public boolean holdsReservation() {
        return this == PENDING || this == CONFIRMED || this == PAID;
    }
}
