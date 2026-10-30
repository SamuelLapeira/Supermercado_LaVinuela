package com.lavinuela.shop.identity.infrastructure.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/register}. Las reglas de fondo (formato, contraseñas comunes...) las valida el dominio. */
public record RegisterRequest(
        @NotBlank(message = "Escribe un nombre de usuario.") @Size(max = 64, message = "El nombre de usuario es demasiado largo.") String username,
        @NotBlank(message = "Escribe tu correo electrónico.") @Size(max = 254, message = "El correo es demasiado largo.") String email,
        @NotBlank(message = "Escribe una contraseña.") @Size(max = 512, message = "La contraseña es demasiado larga.") String password,
        @AssertTrue(message = "Tienes que aceptar las condiciones y la política de privacidad.") boolean acceptTerms) {

    /** La contraseña nunca se muestra en los registros. */
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=***, password=***]";
    }
}
