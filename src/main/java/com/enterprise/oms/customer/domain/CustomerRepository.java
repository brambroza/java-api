package com.enterprise.oms.customer.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Persistence port; implemented by Spring Data JPA in the infrastructure layer. */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByEmail(String email);

    boolean existsById(UUID id);

    Page<Customer> findAll(Pageable pageable);
}
