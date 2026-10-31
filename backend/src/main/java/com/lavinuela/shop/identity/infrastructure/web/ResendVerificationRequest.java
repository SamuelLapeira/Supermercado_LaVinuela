package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/resend-verification}. */
@Schema(name = "ResendVerificationRequest", description = "Petición de un código nuevo")
public record ResendVerificationRequest(
        @Schema(description = "Correo con el que se registró", example = "ana@example.com", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email) {

    @Override
    public String toString() {
        return "ResendVerificationRequest[email=***]";
    }
}
