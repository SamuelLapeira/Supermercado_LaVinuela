package com.lavinuela.shop.shared.domain;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Identificador público de 26 caracteres (formato ULID) que se muestra en la API y en las URL.
 *
 * <p>Los identificadores internos de la base de datos son secuenciales y nunca salen del sistema:
 * exponerlos permitiría adivinar los de otros recursos. Un ULID no es adivinable y, además, se
 * ordena por fecha de creación (los 10 primeros caracteres son el instante en milisegundos).
 *
 * <p>Alfabeto de Crockford (sin I, L, O ni U) para evitar confusiones al leerlo o dictarlo.
 *
 * @param value los 26 caracteres, en mayúsculas
 */
public record PublicId(String value) implements Comparable<PublicId> {

    /** Longitud de todo identificador público. */
    public static final int LENGTH = 26;

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final Pattern FORMAT = Pattern.compile("^[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    private static final int TIME_CHARS = 10;
    private static final long MAX_TIME = (1L << 48) - 1;

    public PublicId {
        Objects.requireNonNull(value, "El identificador es obligatorio");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Identificador público no válido");
        }
    }

    /**
     * Genera un identificador nuevo.
     *
     * @param clock reloj de la aplicación (se inyecta para poder probar con fechas fijas)
     * @param random fuente de aleatoriedad; en producción, un {@link java.security.SecureRandom}
     */
    public static PublicId generate(Clock clock, RandomGenerator random) {
        Objects.requireNonNull(clock, "El reloj es obligatorio");
        Objects.requireNonNull(random, "La fuente aleatoria es obligatoria");
        long time = clock.millis();
        if (time < 0 || time > MAX_TIME) {
            throw new IllegalStateException("La hora del reloj está fuera del rango representable");
        }
        char[] chars = new char[LENGTH];
        long remaining = time;
        for (int i = TIME_CHARS - 1; i >= 0; i--) {
            chars[i] = ALPHABET.charAt((int) (remaining & 31));
            remaining >>>= 5;
        }
        for (int i = TIME_CHARS; i < LENGTH; i++) {
            chars[i] = ALPHABET.charAt(random.nextInt(32));
        }
        return new PublicId(new String(chars));
    }

    /** Interpreta un texto recibido de fuera (la API, una URL). Falla si no es un identificador válido. */
    public static PublicId parse(String text) {
        Objects.requireNonNull(text, "El identificador es obligatorio");
        return new PublicId(text.trim().toUpperCase(Locale.ROOT));
    }

    /** Indica si un texto tiene el formato de un identificador público, sin lanzar excepciones. */
    public static boolean isValid(String text) {
        return text != null && FORMAT.matcher(text.trim().toUpperCase(Locale.ROOT)).matches();
    }

    @Override
    public int compareTo(PublicId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
