package com.lavinuela.shop.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Forma del error estándar de la API ({@code application/problem+json}, RFC 9457). Esta clase <b>solo existe para
 * documentar el contrato</b> (el generador del cliente de Angular la convierte en un tipo): los errores reales los
 * construye {@link GlobalExceptionHandler}.
 */
@Schema(name = "Problem", description = "Error estándar de la API. Siempre lleva un «code» estable con el que el frontend decide qué mostrar.")
public record ProblemResponse(
        @Schema(description = "Identificador del tipo de error", example = "urn:lavinuela:problem:validation-failed", requiredMode = RequiredMode.REQUIRED) String type,
        @Schema(description = "Resumen corto del error", requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(description = "Código HTTP", example = "400", requiredMode = RequiredMode.REQUIRED) int status,
        @Schema(description = "Mensaje pensado para mostrarse a la persona, en español y sin detalles técnicos", requiredMode = RequiredMode.REQUIRED) String detail,
        @Schema(description = "Dirección que ha producido el error", requiredMode = RequiredMode.NOT_REQUIRED) @Nullable String instance,
        @Schema(description = "Código de error estable (por ejemplo VALIDATION_FAILED, USERNAME_TAKEN)", example = "VALIDATION_FAILED", requiredMode = RequiredMode.REQUIRED) String code,
        @Schema(description = "Identificador de seguimiento de la petición: se le pide a quien reporta el problema", requiredMode = RequiredMode.NOT_REQUIRED) @Nullable String traceId,
        @Schema(description = "Errores por campo (solo en VALIDATION_FAILED)", requiredMode = RequiredMode.NOT_REQUIRED) @Nullable List<FieldError> errors,
        @Schema(description = "Datos adicionales de algunos errores", requiredMode = RequiredMode.NOT_REQUIRED) @Nullable Map<String, Object> details) {

    /** Un error asociado a un campo del formulario. */
    @Schema(name = "FieldError")
    public record FieldError(
            @Schema(description = "Nombre del campo", example = "password", requiredMode = RequiredMode.REQUIRED) String field,
            @Schema(description = "Qué está mal, en español", example = "La contraseña debe incluir al menos una mayúscula.", requiredMode = RequiredMode.REQUIRED) String message,
            @Schema(description = "Código técnico del error, si lo hay", requiredMode = RequiredMode.NOT_REQUIRED) @Nullable String code) {
    }
}
