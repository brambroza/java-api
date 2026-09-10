package com.enterprise.oms.ordering.infrastructure;

import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.ordering.domain.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, UUID>, OrderRepository {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByOrderNumber(String orderNumber);
}
