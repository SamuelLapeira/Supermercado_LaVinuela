package com.lavinuela.shop.identity.infrastructure.web;

/** Respuesta de {@code POST /api/v1/auth/resend-verification}: siempre la misma, exista o no la cuenta. */
public record ResendVerificationResponse(int resendAfterSeconds) {
}
