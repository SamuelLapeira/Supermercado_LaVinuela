package com.lavinuela.shop.identity.infrastructure.web;

import com.lavinuela.shop.identity.application.RegisterCustomerCommand;
import com.lavinuela.shop.identity.application.RegisterCustomerService;
import com.lavinuela.shop.identity.application.RegistrationResult;
import com.lavinuela.shop.identity.application.ResendVerificationService;
import com.lavinuela.shop.identity.application.VerifyEmailService;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import com.lavinuela.shop.shared.infrastructure.web.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * {@code application/problem+json} el manejador global de errores. Las anotaciones de OpenAPI describen el contrato
 * del que se genera el cliente de Angular: si se cambia el comportamiento, hay que cambiarlas también (y la prueba
 * del contrato avisa de que el documento ha cambiado).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Cuentas", description = "Registro de cuentas y verificación del correo electrónico")
class AuthController {

    private static final String PROBLEM_JSON = "application/problem+json";

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
    @Operation(operationId = "registerAccount", summary = "Crear una cuenta",
            description = "Crea la cuenta, que queda pendiente de verificar, y envía un código de 6 cifras al correo. "
                    + "La respuesta es la misma tanto si el correo era nuevo como si ya tenía cuenta, para no revelar quién está registrado.")
    @ApiResponse(responseCode = "202", description = "Cuenta creada (o correo ya existente): el código está en camino",
            content = @Content(schema = @Schema(implementation = RegistrationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos incorrectos (VALIDATION_FAILED): «errors» indica el campo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "El nombre de usuario ya está en uso (USERNAME_TAKEN)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResult result = register.register(new RegisterCustomerCommand(
                request.username(), request.email(), request.password(), request.acceptTerms()));
        return new RegistrationResponse(result.maskedEmail(), result.codeValiditySeconds(), result.resendAfterSeconds());
    }

    /** Confirma el correo con el código de 6 cifras y activa la cuenta. */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "verifyEmail", summary = "Verificar el correo con el código",
            description = "Activa la cuenta si el código es correcto. Cada código admite 5 intentos fallidos.")
    @ApiResponse(responseCode = "204", description = "Correo verificado: la cuenta está activa")
    @ApiResponse(responseCode = "400", description = "El código no tiene 6 cifras (VALIDATION_FAILED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "422", description = "Código incorrecto (VERIFICATION_CODE_INVALID) o caducado (VERIFICATION_CODE_EXPIRED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "429", description = "Demasiados intentos con este código (VERIFICATION_TOO_MANY_ATTEMPTS): hay que pedir uno nuevo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmail.verify(request.email(), request.code());
    }

    /** Reenvía el código. Responde siempre lo mismo, exista o no la cuenta. */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "resendVerification", summary = "Pedir un código nuevo",
            description = "Envía un código nuevo si la cuenta está pendiente y no se ha superado ningún límite "
                    + "(60 segundos entre envíos, 5 por hora). Responde siempre lo mismo, exista o no la cuenta.")
    @ApiResponse(responseCode = "202", description = "Petición recibida",
            content = @Content(schema = @Schema(implementation = ResendVerificationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Correo con formato incorrecto (VALIDATION_FAILED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    ResendVerificationResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerification.resend(request.email());
        return new ResendVerificationResponse((int) VerificationPolicy.RESEND_COOLDOWN.toSeconds());
    }
}
