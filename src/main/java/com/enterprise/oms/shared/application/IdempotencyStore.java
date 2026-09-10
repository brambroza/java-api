package com.enterprise.oms.shared.application;

import java.time.Instant;
import java.util.Optional;

/** Persistence port for idempotency keys. Implementations must run each call in its own transaction. */
public interface IdempotencyStore {

    enum Status { IN_PROGRESS, COMPLETED }

    record Entry(String key, String requestHash, Status status, String responseBody, Instant expiresAt) {
    }

    Optional<Entry> find(String key);

    /**
     * Inserts an IN_PROGRESS row. Throws {@link org.springframework.dao.DataIntegrityViolationException}
     * when the key already exists (unique constraint), which the caller uses to detect a replay.
     */
    void claim(String key, String requestHash, Instant expiresAt);

    void complete(String key, String responseBody);

    void release(String key);

    int purgeExpired(Instant now);
}
