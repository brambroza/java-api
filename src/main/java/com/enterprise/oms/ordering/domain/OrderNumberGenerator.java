package com.enterprise.oms.ordering.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Human-readable, non-guessable order numbers: {@code ORD-20260910-7K3QX9AB}. Generated in the
 * application (no database sequence) so it behaves identically on every vendor; uniqueness is
 * guaranteed by the {@code uk_orders_number} constraint.
 */
public class OrderNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;

    public OrderNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return "ORD-" + LocalDate.now(clock).format(DATE) + "-" + suffix;
    }
}
