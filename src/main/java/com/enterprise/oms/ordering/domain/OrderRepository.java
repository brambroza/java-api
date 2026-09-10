package com.enterprise.oms.ordering.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    Order save(Order order);

    /** Loads the order together with its items (no N+1). */
    Optional<Order> findById(UUID id);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findAll(Pageable pageable);
}
