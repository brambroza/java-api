package com.enterprise.oms.catalog.domain;

import com.enterprise.oms.shared.domain.AggregateRoot;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.Money;
import com.enterprise.oms.shared.domain.UuidV7;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product extends AggregateRoot {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, length = 64, unique = true)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount", nullable = false, precision = 19, scale = 4))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", nullable = false, length = 3))
    private Money price;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Product() {
    }

    private Product(UUID id, String sku, String name, String description, Money price) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = true;
    }

    public static Product create(String sku, String name, String description, Money price) {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(name, "name");
        requireNonNegative(price);
        return new Product(UuidV7.generate(), sku.trim().toUpperCase(), name.trim(), description, price);
    }

    public void changePrice(Money newPrice) {
        requireNonNegative(newPrice);
        this.price = newPrice;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static void requireNonNegative(Money price) {
        Objects.requireNonNull(price, "price");
        if (price.negative()) {
            throw new BusinessRuleViolationException("NEGATIVE_PRICE", "Product price must not be negative");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
