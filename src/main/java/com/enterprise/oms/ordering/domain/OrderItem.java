package com.enterprise.oms.ordering.domain;

import com.enterprise.oms.shared.domain.Money;
import com.enterprise.oms.shared.domain.UuidV7;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Line of an order. Product data is copied (snapshot) so later catalog changes never rewrite history. */
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount", nullable = false, precision = 19, scale = 4))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", nullable = false, length = 3))
    private Money unitPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "line_total_amount", nullable = false, precision = 19, scale = 4))
    @AttributeOverride(name = "currency", column = @Column(name = "line_total_currency", nullable = false, length = 3))
    private Money lineTotal;

    protected OrderItem() {
    }

    OrderItem(Order order, int lineNumber, OrderLine line) {
        this.id = UuidV7.generate();
        this.order = order;
        this.lineNumber = lineNumber;
        this.productId = line.productId();
        this.sku = line.sku();
        this.productName = line.productName();
        this.quantity = line.quantity();
        this.unitPrice = line.unitPrice();
        this.lineTotal = line.unitPrice().multiply(line.quantity());
    }

    public UUID getId() {
        return id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getLineTotal() {
        return lineTotal;
    }
}
