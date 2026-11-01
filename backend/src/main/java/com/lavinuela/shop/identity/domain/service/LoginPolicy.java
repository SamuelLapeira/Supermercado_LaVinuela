package com.lavinuela.shop.identity.domain.service;

import java.time.Duration;

/** Reglas del inicio de sesión y de las sesiones. */
public final class LoginPolicy {

    /** Lo que vale un token de acceso: corto a propósito, porque no se puede anular antes de que caduque. */
    public static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(15);

    /** Lo que vale un token de renovación sin usarse. Cada vez que se usa se cambia por otro nuevo. */
    public static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(30);

    /** Intentos fallidos con un mismo correo antes de bloquear. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** Ventana en la que se cuentan los fallos y duración del bloqueo. */
    public static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private LoginPolicy() {
    }
}
