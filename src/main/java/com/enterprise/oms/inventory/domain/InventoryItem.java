package com.enterprise.oms.inventory.domain;

import com.enterprise.oms.shared.domain.AggregateRoot;
import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Stock position of one product. Protected by optimistic locking ({@code @Version} in the base class):
 * two concurrent orders for the last unit cannot both succeed on any vendor, with no explicit DB locks.
 */
@Entity
@Table(name = "inventory_item")
public class InventoryItem extends AggregateRoot {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    protected InventoryItem() {
    }

    private InventoryItem(UUID id, UUID productId, int quantityOnHand) {
        this.id = id;
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
    }

    public static InventoryItem create(UUID productId, int initialQuantity) {
        requirePositiveOrZero(initialQuantity);
        return new InventoryItem(UuidV7.generate(), productId, initialQuantity);
    }

    public int available() {
        return quantityOnHand - quantityReserved;
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (available() < quantity) {
            throw new BusinessRuleViolationException("INSUFFICIENT_STOCK",
                    "Insufficient stock for product %s: requested %d, available %d".formatted(productId, quantity, available()));
        }
        quantityReserved += quantity;
    }

    public void release(int quantity) {
        requirePositive(quantity);
        quantityReserved = Math.max(0, quantityReserved - quantity);
    }

    /** Reserved stock leaves the warehouse (order shipped). */
    public void commit(int quantity) {
        requirePositive(quantity);
        if (quantityReserved < quantity) {
            throw new BusinessRuleViolationException("RESERVATION_MISMATCH",
                    "Cannot commit %d units of product %s: only %d reserved".formatted(quantity, productId, quantityReserved));
        }
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
    }

    public void restock(int quantity) {
        requirePositive(quantity);
        quantityOnHand += quantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("INVALID_QUANTITY", "Quantity must be positive");
        }
    }

    private static void requirePositiveOrZero(int quantity) {
        if (quantity < 0) {
            throw new BusinessRuleViolationException("INVALID_QUANTITY", "Quantity must not be negative");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }
}
