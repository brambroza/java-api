package com.enterprise.oms.ordering.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.oms.catalog.application.ProductDtos.ProductSnapshot;
import com.enterprise.oms.catalog.application.ProductService;
import com.enterprise.oms.customer.application.CustomerService;
import com.enterprise.oms.inventory.application.InventoryService;
import com.enterprise.oms.ordering.application.OrderDtos.OrderResponse;
import com.enterprise.oms.ordering.application.OrderDtos.PlaceOrderRequest;
import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.ordering.domain.OrderNumberGenerator;
import com.enterprise.oms.ordering.domain.OrderRepository;
import com.enterprise.oms.ordering.domain.OrderStatus;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);

    @Mock OrderRepository orderRepository;
    @Mock CustomerService customerService;
    @Mock ProductService productService;
    @Mock InventoryService inventoryService;

    private OrderService service;
    private final UUID customerId = UUID.randomUUID();
    private final UUID laptop = UUID.randomUUID();
    private final UUID mouse = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, customerService, productService, inventoryService,
                new OrderNumberGenerator(CLOCK), CLOCK);
    }

    @Test
    void placesOrderReservingStockAndPersisting() {
        when(productService.findSnapshots(anyCollection())).thenReturn(List.of(
                new ProductSnapshot(laptop, "LAPTOP", "Laptop", Money.of("1000", "THB"), true),
                new ProductSnapshot(mouse, "MOUSE", "Mouse", Money.of("20", "THB"), true)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceOrderRequest request = new PlaceOrderRequest(customerId, List.of(
                new PlaceOrderRequest.Line(laptop, 1),
                new PlaceOrderRequest.Line(mouse, 2),
                new PlaceOrderRequest.Line(mouse, 1)));   // duplicate line is merged

        OrderResponse response = service.placeOrder(request);

        verify(customerService).requireExists(customerId);
        verify(inventoryService).reserve(Map.of(laptop, 1, mouse, 3));
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.orderNumber()).startsWith("ORD-20260910-");
        assertThat(response.total()).isEqualTo(Money.of("1060", "THB"));
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void refusesInactiveProductsBeforeTouchingInventory() {
        when(productService.findSnapshots(anyCollection())).thenReturn(List.of(
                new ProductSnapshot(laptop, "LAPTOP", "Laptop", Money.of("1000", "THB"), false)));

        PlaceOrderRequest request = new PlaceOrderRequest(customerId, List.of(new PlaceOrderRequest.Line(laptop, 1)));

        assertThatThrownBy(() -> service.placeOrder(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("PRODUCT_INACTIVE");
        verify(inventoryService, never()).reserve(any());
        verify(orderRepository, never()).save(any());
    }
}
