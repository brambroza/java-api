package com.enterprise.oms.customer.application;

import com.enterprise.oms.customer.application.CustomerDtos.CustomerResponse;
import com.enterprise.oms.customer.application.CustomerDtos.RegisterCustomerRequest;
import com.enterprise.oms.customer.domain.Customer;
import com.enterprise.oms.customer.domain.CustomerRepository;
import com.enterprise.oms.shared.domain.ConflictException;
import com.enterprise.oms.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerResponse register(RegisterCustomerRequest request) {
        repository.findByEmail(request.email().trim().toLowerCase()).ifPresent(existing -> {
            throw new ConflictException("CUSTOMER_EMAIL_EXISTS", "Customer with email %s already exists".formatted(existing.getEmail()));
        });
        Customer customer = repository.save(Customer.register(request.email(), request.fullName()));
        return CustomerResponse.from(customer);
    }

    public CustomerResponse get(UUID id) {
        return repository.findById(id).map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Customer", id));
    }

    public Page<CustomerResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(CustomerResponse::from);
    }

    public void requireExists(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Customer", id);
        }
    }
}
