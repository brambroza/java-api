package com.enterprise.oms.inventory.application;

import com.enterprise.oms.inventory.domain.InventoryItem;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record RestockRequest(@Positive int quantity) {
    }

    public record InventoryResponse(UUID productId, int quantityOnHand, int quantityReserved, int available, Instant updatedAt) {

        public static InventoryResponse from(InventoryItem item) {
            return new InventoryResponse(item.getProductId(), item.getQuantityOnHand(), item.getQuantityReserved(),
                    item.available(), item.getUpdatedAt());
        }
    }
}
