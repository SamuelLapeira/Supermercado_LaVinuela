package com.lavinuela.shop.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Importe en euros con dos decimales.
 *
 * <p>Es la única forma de manejar dinero en el sistema: nunca {@code double} ni {@code float}. El
 * redondeo es siempre {@link RoundingMode#HALF_UP} y se aplica aquí, en un único sitio, de modo que
 * dos partes del código no puedan redondear de forma distinta.
 *
 * <p>Es un valor inmutable: cada operación devuelve un importe nuevo.
 *
 * @param amount importe con exactamente {@value #SCALE} decimales (se ajusta al construir)
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    /** Número de decimales de todo importe. */
    public static final int SCALE = 2;

    /** Redondeo aplicado en todo el sistema. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** Cero euros. */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Money {
        Objects.requireNonNull(amount, "El importe es obligatorio");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    /** Crea un importe a partir de un decimal (se redondea a dos decimales). */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /** Crea un importe a partir de texto con punto decimal, por ejemplo {@code "12.50"}. */
    public static Money of(String amount) {
        Objects.requireNonNull(amount, "El importe es obligatorio");
        return new Money(new BigDecimal(amount.trim()));
    }

    /** Crea un importe a partir de céntimos, por ejemplo {@code 1250} para 12,50 €. */
    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents, SCALE));
    }

    /** Suma una colección de importes. */
    public static Money sum(Iterable<Money> amounts) {
        Money total = ZERO;
        for (Money amount : amounts) {
            total = total.plus(amount);
        }
        return total;
    }

    /** Importe en céntimos (lo que espera Redsys). Falla si no cabe en un {@code long}. */
    public long toCents() {
        return amount.movePointRight(SCALE).longValueExact();
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Multiplica por una cantidad de unidades. */
    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    /** Porcentaje de este importe, por ejemplo {@code percent(10)} es el 10 %. */
    public Money percent(BigDecimal percent) {
        Objects.requireNonNull(percent, "El porcentaje es obligatorio");
        return new Money(amount.multiply(percent).divide(HUNDRED, SCALE, ROUNDING));
    }

    /**
     * Base imponible de un importe con IVA incluido.
     *
     * <p>Por ejemplo, 10,00 € con IVA del 21 % tienen una base de 8,26 € y 1,74 € de IVA.
     *
     * @param vatPercent tipo de IVA en porcentaje (4, 10 o 21), nunca negativo
     */
    public Money netOfGross(BigDecimal vatPercent) {
        Objects.requireNonNull(vatPercent, "El tipo de IVA es obligatorio");
        if (vatPercent.signum() < 0) {
            throw new IllegalArgumentException("El tipo de IVA no puede ser negativo");
        }
        BigDecimal divisor = BigDecimal.ONE.add(vatPercent.divide(HUNDRED));
        return new Money(amount.divide(divisor, SCALE, ROUNDING));
    }

    /** Cuota de IVA contenida en un importe con IVA incluido (importe menos su base imponible). */
    public Money vatOfGross(BigDecimal vatPercent) {
        return minus(netOfGross(vatPercent));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public Money min(Money other) {
        return isLessThan(other) ? this : other;
    }

    public Money max(Money other) {
        return isGreaterThan(other) ? this : other;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    /** Texto con punto decimal y dos decimales, como se guarda y se envía por la API. */
    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
