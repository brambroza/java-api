package com.enterprise.oms.ordering.application;

import com.enterprise.oms.catalog.application.ProductDtos.ProductSnapshot;
import com.enterprise.oms.catalog.application.ProductService;
import com.enterprise.oms.customer.application.CustomerService;
import com.enterprise.oms.inventory.application.InventoryService;
import com.enterprise.oms.ordering.application.OrderDtos.OrderResponse;
import com.enterprise.oms.ordering.application.OrderDtos.PlaceOrderRequest;
import com.enterprise.oms.ordering.domain.Order;
import com.enterprise.oms.ordering.domain.OrderLine;
import com.enterprise.oms.ordering.domain.OrderNumberGenerator;
import com.enterprise.oms.ordering.domain.OrderRepository;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order use cases. One transaction per command: stock reservation, order persistence and the outbox
 * row commit or roll back together on every vendor.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, CustomerService customerService, ProductService productService,
                        InventoryService inventoryService, OrderNumberGenerator orderNumberGenerator, Clock clock) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        customerService.requireExists(request.customerId());

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        request.lines().forEach(line -> quantities.merge(line.productId(), line.quantity(), Integer::sum));

        List<ProductSnapshot> products = productService.findSnapshots(quantities.keySet());
        products.stream().filter(p -> !p.active()).findFirst().ifPresent(inactive -> {
            throw new BusinessRuleViolationException("PRODUCT_INACTIVE",
                    "Product %s is not available for ordering".formatted(inactive.sku()));
        });

        inventoryService.reserve(quantities);

        List<OrderLine> lines = products.stream()
                .map(p -> new OrderLine(p.id(), p.sku(), p.name(), quantities.get(p.id()), p.price()))
                .toList();
        Order order = Order.place(orderNumberGenerator.next(), request.customerId(), lines, clock.instant());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirm(UUID id) {
        Order order = load(id);
        order.confirm(clock.instant());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse markPaid(UUID id) {
        Order order = load(id);
        order.markPaid(clock.instant());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse ship(UUID id) {
        Order order = load(id);
        order.ship(clock.instant());
        inventoryService.commit(order.quantitiesByProduct());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse deliver(UUID id) {
        Order order = load(id);
        order.deliver(clock.instant());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(UUID id, String reason) {
        Order order = load(id);
        boolean releaseStock = order.getStatus().holdsReservation();
        order.cancel(reason, clock.instant());
        if (releaseStock) {
            inventoryService.release(order.quantitiesByProduct());
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse get(UUID id) {
        return OrderResponse.from(load(id));
    }

    public OrderResponse getByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order", orderNumber));
    }

    public Page<OrderResponse> list(UUID customerId, Pageable pageable) {
        Page<Order> page = customerId == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByCustomerId(customerId, pageable);
        return page.map(OrderResponse::from);
    }

    private Order load(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
    }
}
