package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Respuesta de iniciar sesión y de renovarla. El token de renovación <b>no</b> viaja aquí: va en una cookie que
 * JavaScript no puede leer.
 */
@Schema(name = "SessionResponse", description = "Sesión abierta o renovada")
public record SessionResponse(
        @Schema(description = "Token de acceso (JWT). Se envía en cada petición como «Authorization: Bearer ...». No se guarda en el navegador: vive en memoria.", requiredMode = RequiredMode.REQUIRED) String accessToken,
        @Schema(description = "Segundos que vale el token de acceso", example = "900", requiredMode = RequiredMode.REQUIRED) int expiresInSeconds,
        @Schema(requiredMode = RequiredMode.REQUIRED) UserResponse user) {

    /** El token nunca se muestra en los registros. */
    @Override
    public String toString() {
        return "SessionResponse[user=" + user.username() + ", accessToken=***]";
    }
}
