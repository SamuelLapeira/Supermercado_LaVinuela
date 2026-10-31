package com.lavinuela.shop.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/** Respuesta de {@code POST /api/v1/auth/resend-verification}: siempre la misma, exista o no la cuenta. */
@Schema(name = "ResendVerificationResponse", description = "Siempre la misma respuesta, exista o no la cuenta")
public record ResendVerificationResponse(
        @Schema(description = "Segundos que hay que esperar para pedir otro código", example = "60", requiredMode = RequiredMode.REQUIRED) int resendAfterSeconds) {
}
