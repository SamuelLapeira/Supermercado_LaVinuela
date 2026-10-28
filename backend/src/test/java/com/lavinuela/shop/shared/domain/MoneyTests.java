package com.lavinuela.shop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyTests {

    @Test
    void normalizesToTwoDecimals() {
        assertThat(Money.of("12.5")).isEqualTo(Money.of("12.50"));
        assertThat(Money.of("12.5")).hasToString("12.50");
    }

    @Test
    void roundsHalfUpAtCreation() {
        assertThat(Money.of("1.005")).hasToString("1.01");
        assertThat(Money.of("1.004")).hasToString("1.00");
        assertThat(Money.of("-1.005")).hasToString("-1.01");
    }

    @Test
    void convertsFromAndToCents() {
        assertThat(Money.ofCents(1250)).isEqualTo(Money.of("12.50"));
        assertThat(Money.of("12.50").toCents()).isEqualTo(1250L);
        assertThat(Money.of("0.05").toCents()).isEqualTo(5L);
    }

    @Test
    void addsSubtractsAndMultiplies() {
        assertThat(Money.of("1.10").plus(Money.of("2.20"))).isEqualTo(Money.of("3.30"));
        assertThat(Money.of("5.00").minus(Money.of("0.01"))).isEqualTo(Money.of("4.99"));
        assertThat(Money.of("0.95").times(3)).isEqualTo(Money.of("2.85"));
        assertThat(Money.of("2.00").negate()).isEqualTo(Money.of("-2.00"));
    }

    @Test
    void sumsACollection() {
        assertThat(Money.sum(List.of(Money.of("1.10"), Money.of("2.20"), Money.of("3.30"))))
                .isEqualTo(Money.of("6.60"));
        assertThat(Money.sum(List.of())).isEqualTo(Money.ZERO);
    }

    @Test
    void computesPercentages() {
        assertThat(Money.of("200.00").percent(new BigDecimal("15"))).isEqualTo(Money.of("30.00"));
        assertThat(Money.of("9.99").percent(new BigDecimal("10"))).isEqualTo(Money.of("1.00"));
    }

    @Test
    void splitsGrossAmountIntoNetAndVat() {
        assertThat(Money.of("10.00").netOfGross(new BigDecimal("21"))).isEqualTo(Money.of("8.26"));
        assertThat(Money.of("10.00").vatOfGross(new BigDecimal("21"))).isEqualTo(Money.of("1.74"));
        assertThat(Money.of("2.60").netOfGross(new BigDecimal("4"))).isEqualTo(Money.of("2.50"));
        assertThat(Money.of("2.60").vatOfGross(new BigDecimal("4"))).isEqualTo(Money.of("0.10"));
        assertThat(Money.of("1.89").netOfGross(new BigDecimal("10"))).isEqualTo(Money.of("1.72"));
    }

    @Test
    void netPlusVatAlwaysEqualsGross() {
        for (String gross : List.of("0.01", "0.99", "1.89", "4.80", "6.95", "123.45")) {
            for (String vat : List.of("4", "10", "21")) {
                Money amount = Money.of(gross);
                BigDecimal rate = new BigDecimal(vat);
                assertThat(amount.netOfGross(rate).plus(amount.vatOfGross(rate))).isEqualTo(amount);
            }
        }
    }

    @Test
    void rejectsNegativeVat() {
        assertThatThrownBy(() -> Money.of("1").netOfGross(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Money(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void comparesAndClassifies() {
        assertThat(Money.of("3")).isGreaterThan(Money.of("2.99"));
        assertThat(Money.of("-5").isNegative()).isTrue();
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.of("0.01").isPositive()).isTrue();
        assertThat(Money.of("2").min(Money.of("3"))).isEqualTo(Money.of("2"));
        assertThat(Money.of("2").max(Money.of("3"))).isEqualTo(Money.of("3"));
    }
}
