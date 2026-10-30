package com.lavinuela.shop.identity.application;

/**
 * Qué se le cuenta a quien se registra. Es igual tanto si la cuenta es nueva como si el correo ya existía, para no
 * revelar qué correos tienen cuenta.
 *
 * @param maskedEmail correo con la parte local oculta, para decir «te hemos enviado un código a a***@...»
 */
public record RegistrationResult(String maskedEmail, int codeValiditySeconds, int resendAfterSeconds) {
}
