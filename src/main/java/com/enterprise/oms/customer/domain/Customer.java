package com.enterprise.oms.customer.domain;

import com.enterprise.oms.shared.domain.AggregateRoot;
import com.enterprise.oms.shared.domain.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class Customer extends AggregateRoot {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    protected Customer() {
    }

    private Customer(UUID id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }

    public static Customer register(String email, String fullName) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(fullName, "fullName");
        return new Customer(UuidV7.generate(), email.trim().toLowerCase(), fullName.trim());
    }

    public void rename(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, "fullName").trim();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }
}
