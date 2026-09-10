package com.enterprise.oms.shared.infrastructure.idempotency;

import com.enterprise.oms.shared.application.IdempotencyStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "idempotency_key")
public class IdempotencyRecord implements Persistable<String> {

    @Id
    @Column(name = "idem_key", nullable = false, length = 128)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // keep a plain varchar on every vendor (MySQL would otherwise expect ENUM)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStore.Status status;

    // Length.LONG32 selects text / longtext / clob / varchar(max) per vendor (nvarchar(max) when nationalized)
    @Column(name = "response_body", length = Length.LONG32)
    private String responseBody;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Transient
    private boolean isNew = true;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, String requestHash, Instant createdAt, Instant expiresAt) {
        this.key = key;
        this.requestHash = requestHash;
        this.status = IdempotencyStore.Status.IN_PROGRESS;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void complete(String responseBody) {
        this.status = IdempotencyStore.Status.COMPLETED;
        this.responseBody = responseBody;
    }

    public IdempotencyStore.Entry toEntry() {
        return new IdempotencyStore.Entry(key, requestHash, status, responseBody, expiresAt);
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public String getId() {
        return key;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
