package com.enterprise.oms.inventory.application;

import com.enterprise.oms.inventory.application.InventoryDtos.InventoryResponse;
import com.enterprise.oms.inventory.domain.InventoryItem;
import com.enterprise.oms.inventory.domain.InventoryRepository;
import com.enterprise.oms.shared.domain.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InventoryResponse initialize(UUID productId, int initialQuantity) {
        return InventoryResponse.from(repository.save(InventoryItem.create(productId, initialQuantity)));
    }

    @Transactional
    public InventoryResponse restock(UUID productId, int quantity) {
        InventoryItem item = load(productId);
        item.restock(quantity);
        return InventoryResponse.from(repository.save(item));
    }

    public InventoryResponse get(UUID productId) {
        return InventoryResponse.from(load(productId));
    }

    /** Reserves stock for every product in the map (productId -> quantity) or fails atomically. */
    @Transactional
    public void reserve(Map<UUID, Integer> quantities) {
        apply(quantities, InventoryItem::reserve);
    }

    @Transactional
    public void release(Map<UUID, Integer> quantities) {
        apply(quantities, InventoryItem::release);
    }

    @Transactional
    public void commit(Map<UUID, Integer> quantities) {
        apply(quantities, InventoryItem::commit);
    }

    private void apply(Map<UUID, Integer> quantities, BiConsumer<InventoryItem, Integer> operation) {
        if (quantities.isEmpty()) {
            return;
        }
        List<InventoryItem> items = repository.findByProductIdIn(quantities.keySet());
        for (UUID productId : quantities.keySet()) {
            InventoryItem item = items.stream().filter(i -> i.getProductId().equals(productId)).findFirst()
                    .orElseThrow(() -> new NotFoundException("Inventory for product", productId));
            operation.accept(item, quantities.get(productId));
            repository.save(item);
        }
    }

    private InventoryItem load(UUID productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("Inventory for product", productId));
    }
}
