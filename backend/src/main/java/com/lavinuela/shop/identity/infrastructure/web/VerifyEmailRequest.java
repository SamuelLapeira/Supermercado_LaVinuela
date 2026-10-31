package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/verify-email}. */
@Schema(name = "VerifyEmailRequest", description = "Código recibido por correo para activar la cuenta")
public record VerifyEmailRequest(
        @Schema(description = "Correo con el que se registró", example = "ana@example.com", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email,
        @Schema(description = "Código de 6 cifras", example = "482915", pattern = "^[0-9]{6}$", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe el código.") @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 cifras.") String code) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[email=***, code=***]";
    }
}
