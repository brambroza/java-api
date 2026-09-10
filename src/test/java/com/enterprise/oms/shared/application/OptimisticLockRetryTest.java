package com.enterprise.oms.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterprise.oms.shared.domain.ConflictException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class OptimisticLockRetryTest {

    private final OptimisticLockRetry retry = new OptimisticLockRetry();

    @Test
    void retriesUntilTheUnitOfWorkSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        String result = retry.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ObjectOptimisticLockingFailureException("InventoryItem", "x");
            }
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void givesUpWithConflictAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> retry.execute(() -> {
            attempts.incrementAndGet();
            throw new ObjectOptimisticLockingFailureException("InventoryItem", "x");
        }, 2)).isInstanceOf(ConflictException.class).extracting("code").isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void doesNotRetryOtherFailures() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> retry.execute(() -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("not a lock problem");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(attempts).hasValue(1);
    }
}
