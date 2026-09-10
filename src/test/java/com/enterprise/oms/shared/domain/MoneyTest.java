package com.enterprise.oms.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalisesScaleAndCurrency() {
        Money money = Money.of("10.5", "thb");
        assertThat(money.amount()).isEqualByComparingTo("10.5000");
        assertThat(money.amount().scale()).isEqualTo(4);
        assertThat(money.currency()).isEqualTo("THB");
    }

    @Test
    void addsAndMultiplies() {
        Money total = Money.of("10", "USD").multiply(3).add(Money.of("0.25", "USD"));
        assertThat(total).isEqualTo(Money.of("30.25", "USD"));
    }

    @Test
    void rejectsCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of("1", "USD").add(Money.of("1", "EUR")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("USD").hasMessageContaining("EUR");
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "US")).isInstanceOf(IllegalArgumentException.class);
    }
}
