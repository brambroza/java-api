package com.enterprise.oms.ordering.api;

import com.enterprise.oms.ordering.application.OrderDtos.CancelOrderRequest;
import com.enterprise.oms.ordering.application.OrderDtos.OrderResponse;
import com.enterprise.oms.ordering.application.OrderDtos.PlaceOrderRequest;
import com.enterprise.oms.ordering.application.OrderService;
import com.enterprise.oms.ordering.application.PlaceOrderUseCase;
import com.enterprise.oms.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
public class OrderController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final PlaceOrderUseCase placeOrderUseCase;
    private final OrderService service;

    public OrderController(PlaceOrderUseCase placeOrderUseCase, OrderService service) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Place an order (idempotent: retry safely with the same Idempotency-Key)")
    public ResponseEntity<OrderResponse> place(
            @Parameter(description = "Client-generated unique key, e.g. a UUID", required = true)
            @RequestHeader(IDEMPOTENCY_KEY) @Size(min = 8, max = 128) String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse order = placeOrderUseCase.execute(idempotencyKey, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order")
    public OrderResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-number/{orderNumber}")
    @Operation(summary = "Get an order by its business number")
    public OrderResponse getByNumber(@PathVariable String orderNumber) {
        return service.getByNumber(orderNumber);
    }

    @GetMapping
    @Operation(summary = "List orders, optionally for one customer")
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.list(customerId, pageable), r -> r);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "PENDING -> CONFIRMED")
    public OrderResponse confirm(@PathVariable UUID id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "CONFIRMED -> PAID")
    public OrderResponse pay(@PathVariable UUID id) {
        return service.markPaid(id);
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "PAID -> SHIPPED (reserved stock leaves the warehouse)")
    public OrderResponse ship(@PathVariable UUID id) {
        return service.ship(id);
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "SHIPPED -> DELIVERED")
    public OrderResponse deliver(@PathVariable UUID id) {
        return service.deliver(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (releases reserved stock)")
    public OrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return service.cancel(id, request == null ? null : request.reason());
    }
}
