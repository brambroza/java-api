package com.enterprise.oms.shared.domain;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Time-ordered UUID (RFC 9562, version 7). Generated in the application so aggregates know their
 * identity before they are persisted, and so primary keys stay index-friendly on every database
 * vendor (random UUIDv4 keys fragment clustered B-tree indexes on MySQL/SQL Server).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        long msb = (timestamp << 16)
                | 0x7000L
                | (((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL));
        long lsb = ByteBuffer.wrap(random, 2, 8).getLong();
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L; // RFC 4122 variant
        return new UUID(msb, lsb);
    }
}
