package com.enterprise.oms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterprise.oms.catalog.application.ProductDtos.CreateProductRequest;
import com.enterprise.oms.catalog.application.ProductDtos.ProductResponse;
import com.enterprise.oms.catalog.application.ProductService;
import com.enterprise.oms.customer.application.CustomerDtos.CustomerResponse;
import com.enterprise.oms.customer.application.CustomerDtos.RegisterCustomerRequest;
import com.enterprise.oms.customer.application.CustomerService;
import com.enterprise.oms.inventory.application.InventoryService;
import com.enterprise.oms.ordering.application.OrderDtos.OrderResponse;
import com.enterprise.oms.ordering.application.OrderDtos.PlaceOrderRequest;
import com.enterprise.oms.ordering.application.OrderService;
import com.enterprise.oms.ordering.application.PlaceOrderUseCase;
import com.enterprise.oms.ordering.domain.OrderStatus;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.Money;
import com.enterprise.oms.shared.infrastructure.outbox.OutboxEventRepository;
import com.enterprise.oms.shared.infrastructure.outbox.OutboxRelay;
import com.enterprise.oms.shared.infrastructure.persistence.DatabaseVendorInfo;
import com.enterprise.oms.support.RecordingEventPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vendor contract: the same migration + entity mapping + business flow must work unchanged on every
 * supported database. Subclasses only provide the container. Runs with Hibernate ddl-auto=validate,
 * so a mismatch between the vendor migration and the JPA mapping fails the build.
 */
@SpringBootTest
@ActiveProfiles("vendor-it")
abstract class AbstractVendorContractIT {

    @Autowired DatabaseVendorInfo vendorInfo;
    @Autowired Flyway flyway;
    @Autowired CustomerService customerService;
    @Autowired ProductService productService;
    @Autowired InventoryService inventoryService;
    @Autowired PlaceOrderUseCase placeOrder;
    @Autowired OrderService orderService;
    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired RecordingEventPublisher publisher;

    protected abstract String expectedVendor();

    @Test
    void detectsVendorAndAppliesVendorMigrations() {
        assertThat(vendorInfo.vendor()).isEqualTo(expectedVendor());
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void runsTheFullOrderLifecycle() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        CustomerResponse customer = customerService.register(
                new RegisterCustomerRequest("vendor-" + suffix + "@example.com", "Vendor Tester"));
        ProductResponse product = productService.create(new CreateProductRequest(
                "VND-" + suffix, "Vendor product", "ประเภทสินค้าทดสอบ (unicode)", new BigDecimal("99.99"), "USD", 10));

        String key = "vendor-" + UUID.randomUUID();
        PlaceOrderRequest request = new PlaceOrderRequest(customer.id(), List.of(new PlaceOrderRequest.Line(product.id(), 4)));
        OrderResponse placed = placeOrder.execute(key, request);
        OrderResponse replayed = placeOrder.execute(key, request);

        assertThat(replayed.id()).isEqualTo(placed.id());
        assertThat(placed.total()).isEqualTo(Money.of("399.96", "USD"));
        assertThat(inventoryService.get(product.id()).quantityReserved()).isEqualTo(4);

        assertThatThrownBy(() -> placeOrder.execute("vendor-" + UUID.randomUUID(),
                new PlaceOrderRequest(customer.id(), List.of(new PlaceOrderRequest.Line(product.id(), 7)))))
                .isInstanceOf(BusinessRuleViolationException.class);

        orderService.confirm(placed.id());
        orderService.markPaid(placed.id());
        OrderResponse shipped = orderService.ship(placed.id());
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(inventoryService.get(product.id()).quantityOnHand()).isEqualTo(6);
        assertThat(inventoryService.get(product.id()).quantityReserved()).isZero();

        OrderResponse reloaded = orderService.get(placed.id());
        assertThat(reloaded.items()).singleElement().satisfies(item -> {
            assertThat(item.quantity()).isEqualTo(4);
            assertThat(item.unitPrice()).isEqualTo(Money.of("99.99", "USD"));
        });
        assertThat(reloaded.placedAt()).isEqualTo(placed.placedAt());
        assertThat(productService.get(product.id()).description()).isEqualTo("ประเภทสินค้าทดสอบ (unicode)");

        assertThat(outbox.findByAggregateIdOrderByOccurredAtAsc(placed.id().toString()))
                .extracting(e -> e.getEventType())
                .containsExactly("OrderPlacedEvent", "OrderStatusChangedEvent", "OrderStatusChangedEvent", "OrderStatusChangedEvent");
        assertThat(relay.publishPending()).isGreaterThanOrEqualTo(4);
        assertThat(outbox.countByPublishedAtIsNull()).isZero();
        assertThat(publisher.messages()).anyMatch(m -> m.aggregateId().equals(placed.id().toString()));
    }
}
