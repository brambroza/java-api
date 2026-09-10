package com.enterprise.oms.shared.infrastructure.idempotency;

import com.enterprise.oms.shared.application.IdempotencyStore;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyStore store;
    private final Clock clock;

    public IdempotencyCleanupJob(IdempotencyStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 * * * *")
    public void purgeExpiredKeys() {
        int removed = store.purgeExpired(clock.instant());
        if (removed > 0) {
            log.info("Purged {} expired idempotency keys", removed);
        }
    }
}
