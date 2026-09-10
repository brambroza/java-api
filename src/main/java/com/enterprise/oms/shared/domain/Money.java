package com.enterprise.oms.shared.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money value object. Stored as {@code decimal(19,4)} + ISO-4217 code on every vendor.
 * Immutable record; Hibernate instantiates it through the canonical constructor.
 */
@Embeddable
public record Money(BigDecimal amount, String currency) {

    public static final int SCALE = 4;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be an ISO-4217 code: " + currency);
        }
        currency = currency.toUpperCase();
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public boolean negative() {
        return amount.signum() < 0;
    }

    public boolean positive() {
        return amount.signum() > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new BusinessRuleViolationException("CURRENCY_MISMATCH",
                    "Cannot combine %s with %s".formatted(currency, other.currency));
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
