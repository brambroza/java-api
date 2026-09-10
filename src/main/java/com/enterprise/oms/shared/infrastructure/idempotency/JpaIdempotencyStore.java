package com.enterprise.oms.shared.infrastructure.idempotency;

import com.enterprise.oms.shared.application.IdempotencyStore;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every method runs in its own transaction (REQUIRES_NEW) so the claim/complete/release bookkeeping is
 * independent from the business transaction it protects.
 */
@Repository
public class JpaIdempotencyStore implements IdempotencyStore {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public JpaIdempotencyStore(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Entry> find(String key) {
        return repository.findById(key).map(IdempotencyRecord::toEntry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claim(String key, String requestHash, Instant expiresAt) {
        Instant now = clock.instant();
        repository.findById(key)
                .filter(existing -> existing.getExpiresAt().isBefore(now))
                .ifPresent(expired -> {
                    repository.delete(expired);
                    repository.flush();
                });
        repository.saveAndFlush(new IdempotencyRecord(key, requestHash, now, expiresAt));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, String responseBody) {
        repository.findById(key).ifPresent(record -> record.complete(responseBody));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key) {
        repository.deleteById(key);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeExpired(Instant now) {
        return repository.deleteExpired(now);
    }
}
