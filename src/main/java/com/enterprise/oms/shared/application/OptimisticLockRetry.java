package com.enterprise.oms.shared.application;

import com.enterprise.oms.shared.domain.ConflictException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Re-runs a transactional unit of work when it lost an optimistic-lock race ({@code @Version}).
 * Must wrap the transaction boundary, never run inside it.
 */
@Component
public class OptimisticLockRetry {

    private static final Logger log = LoggerFactory.getLogger(OptimisticLockRetry.class);
    private static final int DEFAULT_ATTEMPTS = 3;

    public <T> T execute(Supplier<T> work) {
        return execute(work, DEFAULT_ATTEMPTS);
    }

    public <T> T execute(Supplier<T> work, int maxAttempts) {
        OptimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return work.get();
            } catch (OptimisticLockingFailureException e) {
                last = e;
                log.warn("Optimistic lock conflict (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                backoff(attempt);
            }
        }
        throw new ConflictException("CONCURRENT_MODIFICATION",
                "The resource was modified concurrently; please retry. Cause: " + last.getMessage());
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(20L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
