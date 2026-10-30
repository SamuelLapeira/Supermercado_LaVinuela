package com.lavinuela.shop.identity.domain.model;

/** Estado de una cuenta. Los valores coinciden con la restricción de la columna {@code users.status}. */
public enum UserStatus {
    /** Registrada, a la espera de que se confirme el correo. No puede iniciar sesión. */
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DELETED
}
