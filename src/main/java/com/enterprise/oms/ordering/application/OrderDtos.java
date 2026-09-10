package com.enterprise.oms.ordering.application;

import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.ordering.domain.OrderItem;
import com.enterprise.oms.ordering.domain.OrderStatus;
import com.enterprise.oms.shared.domain.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotNull UUID customerId,
            @NotEmpty @Size(max = 100) List<@Valid Line> lines) {

        public record Line(@NotNull UUID productId, @Positive @Max(10_000) int quantity) {
        }
    }

    public record CancelOrderRequest(@Size(max = 500) String reason) {
    }

    public record OrderItemResponse(int lineNumber, UUID productId, String sku, String productName, int quantity,
                                    Money unitPrice, Money lineTotal) {

        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getLineNumber(), item.getProductId(), item.getSku(), item.getProductName(),
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }

    public record OrderResponse(UUID id, String orderNumber, UUID customerId, OrderStatus status, Money total,
                                List<OrderItemResponse> items, Instant placedAt, String cancelReason,
                                Instant updatedAt, Long version) {

        public static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                    order.getTotal(), order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getPlacedAt(), order.getCancelReason(), order.getUpdatedAt(), order.getVersion());
        }
    }
}
