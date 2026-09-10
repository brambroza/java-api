package com.enterprise.oms.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterprise.oms.shared.domain.BusinessRuleViolationException;
import com.enterprise.oms.shared.domain.ConflictException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

class IdempotencyServiceTest {

    record Request(String item, int qty) {
    }

    record Response(String id, int qty) {
    }

    /** In-memory stand-in for the JPA store with the same unique-key semantics. */
    static class InMemoryStore implements IdempotencyStore {
        final Map<String, Entry> rows = new ConcurrentHashMap<>();

        @Override public Optional<Entry> find(String key) {
            return Optional.ofNullable(rows.get(key));
        }

        @Override public void claim(String key, String requestHash, Instant expiresAt) {
            Entry previous = rows.putIfAbsent(key, new Entry(key, requestHash, Status.IN_PROGRESS, null, expiresAt));
            if (previous != null) {
                throw new DataIntegrityViolationException("duplicate key " + key);
            }
        }

        @Override public void complete(String key, String responseBody) {
            rows.computeIfPresent(key, (k, e) -> new Entry(k, e.requestHash(), Status.COMPLETED, responseBody, e.expiresAt()));
        }

        @Override public void release(String key) {
            rows.remove(key);
        }

        @Override public int purgeExpired(Instant now) {
            return 0;
        }
    }

    private InMemoryStore store;
    private IdempotencyService service;
    private final AtomicInteger executions = new AtomicInteger();

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        AppProperties properties = new AppProperties(
                new AppProperties.Security(false, new AppProperties.Security.Jwt(null)),
                new AppProperties.Scheduling(false),
                new AppProperties.Outbox("log", 50, Duration.ofSeconds(5)),
                new AppProperties.Idempotency(Duration.ofHours(24)),
                false);
        service = new IdempotencyService(store, new ObjectMapper(), properties, Clock.systemUTC());
    }

    private Response action(Request request) {
        executions.incrementAndGet();
        return new Response("resp-" + executions.get(), request.qty());
    }

    @Test
    void executesOnceAndReplaysStoredResponse() {
        Request request = new Request("laptop", 2);

        Response first = service.execute("key-1", request, Response.class, () -> action(request));
        Response replay = service.execute("key-1", request, Response.class, () -> action(request));

        assertThat(executions).hasValue(1);
        assertThat(replay).isEqualTo(first);
        assertThat(store.rows.get("key-1").status()).isEqualTo(IdempotencyStore.Status.COMPLETED);
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        service.execute("key-2", new Request("laptop", 2), Response.class, () -> action(new Request("laptop", 2)));

        assertThatThrownBy(() -> service.execute("key-2", new Request("laptop", 3), Response.class,
                () -> action(new Request("laptop", 3))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(executions).hasValue(1);
    }

    @Test
    void reportsConflictWhileFirstRequestStillRunning() {
        Request request = new Request("laptop", 1);
        store.claim("key-3", IdempotencyService.sha256(new ObjectMapper().writeValueAsString(request)), Instant.MAX);

        assertThatThrownBy(() -> service.execute("key-3", request, Response.class, () -> action(request)))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("REQUEST_IN_PROGRESS");
    }

    @Test
    void releasesKeyWhenActionFailsSoClientCanRetry() {
        Request request = new Request("laptop", 1);

        assertThatThrownBy(() -> service.execute("key-4", request, Response.class, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.rows).doesNotContainKey("key-4");
        Response retried = service.execute("key-4", request, Response.class, () -> action(request));
        assertThat(retried.qty()).isEqualTo(1);
    }
}
