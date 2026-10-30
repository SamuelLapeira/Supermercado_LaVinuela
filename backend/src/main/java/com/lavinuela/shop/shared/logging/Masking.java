package com.lavinuela.shop.shared.logging;

import org.jspecify.annotations.Nullable;

/**
 * Enmascara datos personales antes de escribirlos en un registro.
 *
 * <p>Regla del proyecto: en los registros <b>nunca</b> van contraseñas, tokens, cabeceras de autorización,
 * firmas de Redsys ni datos de tarjeta, y los datos personales (correo, teléfono) solo enmascarados. Para
 * identificar a alguien se usa su identificador público.
 */
public final class Masking {

    private static final String HIDDEN = "***";

    private Masking() {
    }

    /** {@code ana.perez@example.com} pasa a {@code a***@example.com}. */
    public static String email(@Nullable String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return HIDDEN;
        }
        return email.charAt(0) + HIDDEN + email.substring(at);
    }

    /** {@code 600 123 456} pasa a {@code *******56}: solo quedan las dos últimas cifras. */
    public static String phone(@Nullable String phone) {
        if (phone == null || phone.isBlank()) {
            return "-";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 2) {
            return HIDDEN;
        }
        return "*".repeat(digits.length() - 2) + digits.substring(digits.length() - 2);
    }

    /** Oculta por completo un valor secreto (contraseña, token, firma). */
    public static String secret(@Nullable String value) {
        return value == null ? "-" : HIDDEN;
    }
}
