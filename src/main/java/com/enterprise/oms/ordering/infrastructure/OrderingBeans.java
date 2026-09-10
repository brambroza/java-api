package com.enterprise.oms.ordering.infrastructure;

import com.enterprise.oms.ordering.domain.OrderNumberGenerator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires framework-free domain services into the container. */
@Configuration
public class OrderingBeans {

    @Bean
    public OrderNumberGenerator orderNumberGenerator(Clock clock) {
        return new OrderNumberGenerator(clock);
    }
}
