package com.enterprise.oms.inventory.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByProductId(UUID productId);

    List<InventoryItem> findByProductIdIn(Collection<UUID> productIds);
}
