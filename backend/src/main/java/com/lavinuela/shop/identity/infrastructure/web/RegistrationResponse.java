package com.lavinuela.shop.identity.infrastructure.web;

/** Respuesta de {@code POST /api/v1/auth/register}. */
public record RegistrationResponse(String email, int codeValiditySeconds, int resendAfterSeconds) {
}
