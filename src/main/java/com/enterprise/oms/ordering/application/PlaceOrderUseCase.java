package com.enterprise.oms.ordering.application;

import com.enterprise.oms.ordering.application.OrderDtos.OrderResponse;
import com.enterprise.oms.ordering.application.OrderDtos.PlaceOrderRequest;
import com.enterprise.oms.shared.application.IdempotencyService;
import com.enterprise.oms.shared.application.OptimisticLockRetry;
import org.springframework.stereotype.Service;

/**
 * Entry point for "place order": idempotent (safe client retries) and resilient to optimistic-lock
 * races on inventory (automatic re-execution of the whole transaction).
 * Deliberately NOT transactional: both wrappers must sit outside the transaction boundary.
 */
@Service
public class PlaceOrderUseCase {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final OptimisticLockRetry retry;

    public PlaceOrderUseCase(OrderService orderService, IdempotencyService idempotencyService, OptimisticLockRetry retry) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
        this.retry = retry;
    }

    public OrderResponse execute(String idempotencyKey, PlaceOrderRequest request) {
        return idempotencyService.execute(idempotencyKey, request, OrderResponse.class,
                () -> retry.execute(() -> orderService.placeOrder(request)));
    }
}
