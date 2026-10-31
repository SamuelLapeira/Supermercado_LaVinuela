package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/** Respuesta de {@code POST /api/v1/auth/register}. */
@Schema(name = "RegistrationResponse", description = "Resultado del registro. Es igual tanto si el correo era nuevo como si ya existía.")
public record RegistrationResponse(
        @Schema(description = "Correo con la parte local oculta, para decir «te hemos enviado un código a a***@…»", example = "a***@example.com", requiredMode = RequiredMode.REQUIRED) String email,
        @Schema(description = "Segundos que vale el código", example = "900", requiredMode = RequiredMode.REQUIRED) int codeValiditySeconds,
        @Schema(description = "Segundos que hay que esperar para pedir otro código", example = "60", requiredMode = RequiredMode.REQUIRED) int resendAfterSeconds) {
}
