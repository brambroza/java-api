package com.enterprise.oms.customer.infrastructure;

import com.enterprise.oms.customer.domain.Customer;
import com.enterprise.oms.customer.domain.CustomerRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<Customer, UUID>, CustomerRepository {
}
