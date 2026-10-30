package com.lavinuela.shop.identity.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/verify-email}. */
public record VerifyEmailRequest(
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email,
        @NotBlank(message = "Escribe el código.") @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 cifras.") String code) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[email=***, code=***]";
    }
}
