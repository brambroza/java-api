package com.enterprise.oms.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void reservesReleasesAndCommitsStock() {
        InventoryItem item = InventoryItem.create(UUID.randomUUID(), 10);

        item.reserve(4);
        assertThat(item.available()).isEqualTo(6);
        assertThat(item.getQuantityReserved()).isEqualTo(4);

        item.release(1);
        assertThat(item.getQuantityReserved()).isEqualTo(3);

        item.commit(3);
        assertThat(item.getQuantityOnHand()).isEqualTo(7);
        assertThat(item.getQuantityReserved()).isZero();
    }

    @Test
    void refusesToOverReserve() {
        InventoryItem item = InventoryItem.create(UUID.randomUUID(), 2);
        assertThatThrownBy(() -> item.reserve(3))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(item.getQuantityReserved()).isZero();
    }

    @Test
    void refusesToCommitMoreThanReserved() {
        InventoryItem item = InventoryItem.create(UUID.randomUUID(), 5);
        item.reserve(1);
        assertThatThrownBy(() -> item.commit(2)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsNonPositiveQuantities() {
        InventoryItem item = InventoryItem.create(UUID.randomUUID(), 5);
        assertThatThrownBy(() -> item.reserve(0)).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> item.restock(-1)).isInstanceOf(BusinessRuleViolationException.class);
    }
}
