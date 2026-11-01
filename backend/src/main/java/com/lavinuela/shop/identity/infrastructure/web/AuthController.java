package com.lavinuela.shop.identity.infrastructure.web;

import com.lavinuela.shop.identity.application.AuthSession;
import com.lavinuela.shop.identity.application.LoginService;
import com.lavinuela.shop.identity.application.LogoutService;
import com.lavinuela.shop.identity.application.RefreshSessionService;
import com.lavinuela.shop.identity.application.RegisterCustomerCommand;
import com.lavinuela.shop.identity.application.RegisterCustomerService;
import com.lavinuela.shop.identity.application.RegistrationResult;
import com.lavinuela.shop.identity.application.ResendVerificationService;
import com.lavinuela.shop.identity.application.VerifyEmailService;
import com.lavinuela.shop.identity.domain.service.LoginPolicy;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import com.lavinuela.shop.identity.infrastructure.config.IdentityProperties;
import com.lavinuela.shop.shared.error.BusinessRuleViolation;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.infrastructure.web.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
@Tag(name = "Cuentas", description = "Registro de cuentas, verificación del correo e inicio y cierre de sesión")
class AuthController {

    private static final String PROBLEM_JSON = "application/problem+json";

    /**
     * Cabecera que la tienda añade a todas las peticiones. Renovar y cerrar sesión se identifican con una cookie, así que
     * exigen además esta cabecera: una web ajena no puede añadirla (el navegador no se lo permite sin permiso del servidor).
     */
    static final String CLIENT_HEADER = "X-LV-Client";

    private static final String CLIENT_HEADER_VALUE = "web";

    private final RegisterCustomerService register;
    private final VerifyEmailService verifyEmail;
    private final ResendVerificationService resendVerification;
    private final LoginService login;
    private final RefreshSessionService refreshSession;
    private final LogoutService logout;
    private final boolean cookieSecure;

    AuthController(
            RegisterCustomerService register,
            VerifyEmailService verifyEmail,
            ResendVerificationService resendVerification,
            LoginService login,
            RefreshSessionService refreshSession,
            LogoutService logout,
            IdentityProperties properties) {
        this.register = register;
        this.verifyEmail = verifyEmail;
        this.resendVerification = resendVerification;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.cookieSecure = properties.refreshCookieSecure();
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

    /** Inicia sesión con el correo y la contraseña. */
    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Iniciar sesión",
            description = "Devuelve un token de acceso (15 minutos) y deja en una cookie protegida el token de renovación. "
                    + "Tras 5 intentos fallidos con un mismo correo se bloquea durante 15 minutos. La respuesta es la misma "
                    + "si el correo no existe o la contraseña es incorrecta.")
    @ApiResponse(responseCode = "200", description = "Sesión abierta",
            content = @Content(schema = @Schema(implementation = SessionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos incorrectos (VALIDATION_FAILED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos (AUTH_INVALID_CREDENTIALS)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "403", description = "La contraseña es correcta pero falta confirmar el correo (AUTH_EMAIL_NOT_VERIFIED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "423", description = "Demasiados intentos fallidos (AUTH_ACCOUNT_LOCKED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    ResponseEntity<SessionResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.USER_AGENT, required = false) @Nullable String userAgent) {
        return sessionResponse(login.login(request.email(), request.password(), userAgent));
    }

    /** Renueva la sesión con la cookie del token de renovación. */
    @PostMapping("/refresh")
    @Operation(operationId = "refreshSession", summary = "Renovar la sesión",
            description = "Cambia el token de renovación (que viaja en una cookie que JavaScript no puede leer) por uno nuevo, "
                    + "y entrega un token de acceso nuevo. Cada token de renovación vale una sola vez: si se reutiliza uno "
                    + "ya usado, se cierra la sesión entera por seguridad. Exige la cabecera X-LV-Client: web.")
    @ApiResponse(responseCode = "200", description = "Sesión renovada",
            content = @Content(schema = @Schema(implementation = SessionResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión, caducada (AUTH_TOKEN_EXPIRED) o reutilizada (AUTH_TOKEN_REUSED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "403", description = "Falta la cabecera X-LV-Client (ACCESS_DENIED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    ResponseEntity<SessionResponse> refresh(
            @Parameter(hidden = true) @CookieValue(name = RefreshCookie.NAME, required = false) @Nullable String refreshToken,
            @Parameter(hidden = true) @RequestHeader(name = CLIENT_HEADER, required = false) @Nullable String client,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.USER_AGENT, required = false) @Nullable String userAgent) {
        requireWebClient(client);
        return sessionResponse(refreshSession.refresh(refreshToken, userAgent));
    }

    /** Cierra la sesión: anula el token de renovación y borra la cookie. */
    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Cerrar sesión",
            description = "Anula la sesión y borra la cookie del token de renovación. Es idempotente: si no hay sesión, no pasa nada. "
                    + "Exige la cabecera X-LV-Client: web.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @ApiResponse(responseCode = "403", description = "Falta la cabecera X-LV-Client (ACCESS_DENIED)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    ResponseEntity<Void> logout(
            @Parameter(hidden = true) @CookieValue(name = RefreshCookie.NAME, required = false) @Nullable String refreshToken,
            @Parameter(hidden = true) @RequestHeader(name = CLIENT_HEADER, required = false) @Nullable String client) {
        requireWebClient(client);
        logout.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.clear(cookieSecure).toString())
                .build();
    }

    private ResponseEntity<SessionResponse> sessionResponse(AuthSession session) {
        SessionResponse body = new SessionResponse(
                session.accessToken(),
                (int) LoginPolicy.ACCESS_TOKEN_VALIDITY.toSeconds(),
                new UserResponse(session.user().id(), session.user().username(), session.user().email(), session.user().roles()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.issue(session.refreshToken(), LoginPolicy.REFRESH_TOKEN_VALIDITY, cookieSecure).toString())
                .body(body);
    }

    private static void requireWebClient(@Nullable String client) {
        if (!CLIENT_HEADER_VALUE.equals(client)) {
            throw new BusinessRuleViolation(ErrorCode.ACCESS_DENIED);
        }
    }
}
