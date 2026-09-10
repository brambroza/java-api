package com.enterprise.oms.bootstrap;

import com.enterprise.oms.catalog.application.ProductDtos.CreateProductRequest;
import com.enterprise.oms.catalog.application.ProductService;
import com.enterprise.oms.customer.application.CustomerDtos.RegisterCustomerRequest;
import com.enterprise.oms.customer.application.CustomerService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Sample data for local development (enabled with {@code app.seed-data=true}). */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class LocalDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSeeder.class);

    private final CustomerService customerService;
    private final ProductService productService;

    public LocalDataSeeder(CustomerService customerService, ProductService productService) {
        this.customerService = customerService;
        this.productService = productService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var alice = customerService.register(new RegisterCustomerRequest("alice@example.com", "Alice Anderson"));
        var bob = customerService.register(new RegisterCustomerRequest("bob@example.com", "Bob Brown"));
        var laptop = productService.create(new CreateProductRequest("LAPTOP-15", "Laptop 15\"", "Business laptop",
                new BigDecimal("32900.00"), "THB", 25));
        var mouse = productService.create(new CreateProductRequest("MOUSE-WL", "Wireless Mouse", null,
                new BigDecimal("590.00"), "THB", 500));
        var monitor = productService.create(new CreateProductRequest("MON-27-4K", "27\" 4K Monitor", null,
                new BigDecimal("12900.00"), "THB", 3));
        log.info("Seeded sample data: customers [{} , {}], products [{}, {}, {}]",
                alice.id(), bob.id(), laptop.id(), mouse.id(), monitor.id());
    }
}
