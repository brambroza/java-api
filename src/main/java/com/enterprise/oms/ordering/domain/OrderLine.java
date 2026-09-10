package com.enterprise.oms.ordering.domain;

import com.enterprise.oms.shared.domain.Money;
import java.util.UUID;

/** Input for placing an order: a product snapshot plus the requested quantity. */
public record OrderLine(UUID productId, String sku, String productName, int quantity, Money unitPrice) {
}
