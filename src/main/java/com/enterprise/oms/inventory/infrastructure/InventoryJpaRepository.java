package com.enterprise.oms.inventory.infrastructure;

import com.enterprise.oms.inventory.domain.InventoryItem;
import com.enterprise.oms.inventory.domain.InventoryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryJpaRepository extends JpaRepository<InventoryItem, UUID>, InventoryRepository {
}
