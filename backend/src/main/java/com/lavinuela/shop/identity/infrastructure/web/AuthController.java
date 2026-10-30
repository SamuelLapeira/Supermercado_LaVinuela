package com.lavinuela.shop.identity.infrastructure.web;

import com.lavinuela.shop.identity.application.RegisterCustomerCommand;
import com.lavinuela.shop.identity.application.RegisterCustomerService;
import com.lavinuela.shop.identity.application.RegistrationResult;
import com.lavinuela.shop.identity.application.ResendVerificationService;
import com.lavinuela.shop.identity.application.VerifyEmailService;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y verificación del correo. El controlador solo traduce HTTP a casos de uso: no tiene lógica de negocio.
 *
 * <p>Los errores (nombre de usuario cogido, código incorrecto o caducado...) los convierte en respuestas
 * {@code application/problem+json} el manejador global de errores.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegisterCustomerService register;
    private final VerifyEmailService verifyEmail;
    private final ResendVerificationService resendVerification;

    AuthController(
            RegisterCustomerService register,
            VerifyEmailService verifyEmail,
            ResendVerificationService resendVerification) {
        this.register = register;
        this.verifyEmail = verifyEmail;
        this.resendVerification = resendVerification;
    }

    /** Crea la cuenta (pendiente de verificar) y envía el código por correo. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResult result = register.register(new RegisterCustomerCommand(
                request.username(), request.email(), request.password(), request.acceptTerms()));
        return new RegistrationResponse(result.maskedEmail(), result.codeValiditySeconds(), result.resendAfterSeconds());
    }

    /** Confirma el correo con el código de 6 cifras y activa la cuenta. */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmail.verify(request.email(), request.code());
    }

    /** Reenvía el código. Responde siempre lo mismo, exista o no la cuenta. */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ResendVerificationResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerification.resend(request.email());
        return new ResendVerificationResponse((int) VerificationPolicy.RESEND_COOLDOWN.toSeconds());
    }
}
