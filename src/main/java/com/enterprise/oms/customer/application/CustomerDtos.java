package com.enterprise.oms.customer.application;

import com.enterprise.oms.customer.domain.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record RegisterCustomerRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 200) String fullName) {
    }

    public record CustomerResponse(UUID id, String email, String fullName, Instant createdAt, Instant updatedAt) {

        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getFullName(),
                    customer.getCreatedAt(), customer.getUpdatedAt());
        }
    }
}
