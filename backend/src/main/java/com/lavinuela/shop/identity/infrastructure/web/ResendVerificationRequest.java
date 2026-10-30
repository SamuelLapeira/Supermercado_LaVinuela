package com.lavinuela.shop.identity.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/resend-verification}. */
public record ResendVerificationRequest(
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email) {

    @Override
    public String toString() {
        return "ResendVerificationRequest[email=***]";
    }
}
