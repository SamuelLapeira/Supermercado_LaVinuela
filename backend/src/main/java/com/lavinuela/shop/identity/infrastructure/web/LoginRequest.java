package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/login}. Se entra con el correo electrónico y la contraseña. */
@Schema(name = "LoginRequest", description = "Datos para iniciar sesión")
public record LoginRequest(
        @Schema(description = "Correo electrónico de la cuenta. No distingue mayúsculas.", example = "ana@example.com", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email,
        @Schema(description = "Contraseña de la cuenta", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe tu contraseña.") @Size(max = 512, message = "La contraseña es demasiado larga.") String password) {

    /** La contraseña nunca se muestra en los registros. */
    @Override
    public String toString() {
        return "LoginRequest[email=***, password=***]";
    }
}
