package com.enterprise.oms.customer.api;

import com.enterprise.oms.customer.application.CustomerDtos.CustomerResponse;
import com.enterprise.oms.customer.application.CustomerDtos.RegisterCustomerRequest;
import com.enterprise.oms.customer.application.CustomerService;
import com.enterprise.oms.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a customer")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        CustomerResponse created = service.register(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer")
    public CustomerResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List customers")
    public PageResponse<CustomerResponse> list(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.list(pageable), r -> r);
    }
}
