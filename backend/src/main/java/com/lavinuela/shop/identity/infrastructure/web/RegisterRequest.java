package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/register}. Las reglas de fondo (formato, contraseñas comunes...) las valida el dominio. */
@Schema(name = "RegisterRequest", description = "Datos para crear una cuenta")
public record RegisterRequest(
        @Schema(description = "Nombre de usuario: de 3 a 30 caracteres (letras, números, punto, guion y guion bajo). No distingue mayúsculas.", example = "ana.lopez", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe un nombre de usuario.") @Size(max = 64, message = "El nombre de usuario es demasiado largo.") String username,
        @Schema(description = "Correo electrónico, donde se envía el código de verificación", example = "ana@example.com", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email,
        @Schema(description = "Contraseña: de 10 a 128 caracteres, con al menos una mayúscula, una minúscula y un número", example = "Una-contraseña-larga-2026", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "Escribe una contraseña.") @Size(max = 512, message = "La contraseña es demasiado larga.") String password,
        @Schema(description = "Debe ser verdadero: la persona acepta las condiciones y la política de privacidad", example = "true", requiredMode = RequiredMode.REQUIRED)
        @AssertTrue(message = "Tienes que aceptar las condiciones y la política de privacidad.") boolean acceptTerms) {

    /** La contraseña nunca se muestra en los registros. */
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=***, password=***]";
    }
}
