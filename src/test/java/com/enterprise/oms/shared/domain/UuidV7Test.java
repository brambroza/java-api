package com.enterprise.oms.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void producesVersion7Rfc4122Uuids() {
        UUID id = UuidV7.generate();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void embedsTimestampInHighBits() {
        long before = System.currentTimeMillis();
        UUID id = UuidV7.generate();
        long embedded = id.getMostSignificantBits() >>> 16;
        assertThat(embedded).isBetween(before, System.currentTimeMillis());
    }

    @Test
    void isUniqueAcrossManyGenerations() {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(UuidV7.generate());
        }
        assertThat(ids).hasSize(10_000);
    }
}
