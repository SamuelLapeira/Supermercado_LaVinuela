package com.lavinuela.shop.identity.domain.service;

import java.time.Duration;

/** Reglas del código de verificación de correo. */
public final class VerificationPolicy {

    /** Cifras del código. */
    public static final int CODE_LENGTH = 6;

    /** Tiempo que vale un código. */
    public static final Duration VALIDITY = Duration.ofMinutes(15);

    /** Intentos fallidos permitidos con un mismo código; al llegar a este número hay que pedir otro. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** Espera mínima entre dos códigos para la misma cuenta. */
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    /** Códigos que se pueden emitir para una cuenta en una hora. */
    public static final int MAX_CODES_PER_HOUR = 5;

    private VerificationPolicy() {
    }
}
