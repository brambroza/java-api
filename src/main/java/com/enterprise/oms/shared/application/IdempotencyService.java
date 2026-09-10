package com.enterprise.oms.shared.application;

import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Idempotent execution of non-idempotent commands (Stripe-style {@code Idempotency-Key}).
 * <ol>
 *   <li>Claim the key (INSERT, own transaction). Duplicate key = replay.</li>
 *   <li>Same key + same request hash + COMPLETED: return the stored response, do not re-execute.</li>
 *   <li>Same key + different payload: 422. Same key still IN_PROGRESS: 409.</li>
 *   <li>Run the action; store the response, or release the key on failure so the client can retry.</li>
 * </ol>
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyStore store;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final Clock clock;

    public IdempotencyService(IdempotencyStore store, ObjectMapper objectMapper, AppProperties properties, Clock clock) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public <T> T execute(String key, Object request, Class<T> responseType, Supplier<T> action) {
        String requestHash = sha256(objectMapper.writeValueAsString(request));
        try {
            store.claim(key, requestHash, clock.instant().plus(properties.idempotency().ttl()));
        } catch (DataIntegrityViolationException duplicate) {
            return replay(key, requestHash, responseType, duplicate);
        }

        T result;
        try {
            result = action.get();
        } catch (RuntimeException failure) {
            store.release(key);
            throw failure;
        }
        store.complete(key, objectMapper.writeValueAsString(result));
        return result;
    }

    private <T> T replay(String key, String requestHash, Class<T> responseType, RuntimeException cause) {
        IdempotencyStore.Entry entry = store.find(key).orElseThrow(() -> cause);
        if (!entry.requestHash().equals(requestHash)) {
            throw new BusinessRuleViolationException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key '%s' was already used with a different request payload".formatted(key));
        }
        if (entry.status() == IdempotencyStore.Status.IN_PROGRESS) {
            throw new ConflictException("REQUEST_IN_PROGRESS",
                    "A request with Idempotency-Key '%s' is still being processed".formatted(key));
        }
        log.info("Replaying stored response for Idempotency-Key {}", key);
        return objectMapper.readValue(entry.responseBody(), responseType);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
