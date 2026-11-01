package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/** Quién es la persona de la sesión. */
@Schema(name = "UserResponse", description = "La persona que ha iniciado sesión")
public record UserResponse(
        @Schema(description = "Identificador público de la cuenta", example = "01KB3Z6Q8M2R7V1N4P9XJ5C0TD", requiredMode = RequiredMode.REQUIRED) String id,
        @Schema(description = "Nombre de usuario", example = "ana.lopez", requiredMode = RequiredMode.REQUIRED) String username,
        @Schema(description = "Correo electrónico", example = "ana@example.com", requiredMode = RequiredMode.REQUIRED) String email,
        @Schema(description = "Roles de la cuenta (CUSTOMER, ADMIN...)", example = "[\"CUSTOMER\"]", requiredMode = RequiredMode.REQUIRED) List<String> roles) {
}
