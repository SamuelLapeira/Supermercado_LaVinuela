package com.lavinuela.shop.shared.infrastructure.web;

import com.lavinuela.shop.shared.error.CodedException;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.error.FieldViolation;
import com.lavinuela.shop.shared.error.ValidationFailedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte cualquier excepción en una respuesta {@code application/problem+json} (RFC 9457).
 *
 * <p>Reglas:
 * <ul>
 *   <li>El mensaje que ve la persona usuaria es el de {@link ErrorCode}, en español claro.</li>
 *   <li>El detalle técnico, las trazas y los mensajes de SQL <b>nunca</b> salen en la respuesta: van al
 *       registro junto con el identificador de seguimiento ({@code traceId}).</li>
 *   <li>Los errores inesperados responden con un 500 genérico.</li>
 * </ul>
 *
 * <p>Las excepciones de seguridad (autenticación y autorización) se tratarán en la historia E1-02, cuando
 * se añada Spring Security: hasta entonces no existen en la aplicación.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_KEY = "traceId";

    /** Errores propios de la aplicación (reglas de negocio y fallos de servicios externos). */
    @ExceptionHandler(CodedException.class)
    public ResponseEntity<ProblemDetail> handleCoded(CodedException ex) {
        ErrorCode code = ex.code();
        if (code.httpStatus() >= 500) {
            LOG.error("Fallo de un servicio externo [{}] (traceId={})", code, traceId(), ex);
        } else {
            LOG.info("Petición rechazada [{}]: {}", code, ex.getMessage());
        }
        ProblemDetail problem = problem(code, ex.getMessage());
        if (ex instanceof ValidationFailedException validation) {
            problem.setProperty("errors", validation.violations());
        } else if (!ex.details().isEmpty()) {
            problem.setProperty("details", ex.details());
        }
        return response(problem);
    }

    /** Cuerpo de la petición con campos que incumplen la validación ({@code @Valid}). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            violations.add(new FieldViolation(
                    fieldError.getField(), messageOrDefault(fieldError.getDefaultMessage()), fieldError.getCode()));
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            violations.add(new FieldViolation(
                    "", messageOrDefault(globalError.getDefaultMessage()), globalError.getCode()));
        }
        return validationResponse(violations);
    }

    /** Parámetros de ruta o de consulta que incumplen la validación. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.add(new FieldViolation(
                    violation.getPropertyPath().toString(), messageOrDefault(violation.getMessage()), null));
        }
        return validationResponse(violations);
    }

    /** Un parámetro llega con un tipo que no se puede convertir (por ejemplo, texto donde va un número). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return validationResponse(List.of(FieldViolation.of(ex.getName(), "El valor no tiene el formato esperado.")));
    }

    /** Falta un parámetro obligatorio. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex) {
        return validationResponse(List.of(FieldViolation.of(ex.getParameterName(), "Este dato es obligatorio.")));
    }

    /** El cuerpo no es JSON válido o no se puede interpretar. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        LOG.info("Cuerpo de petición ilegible: {}", ex.getMessage());
        return response(problem(ErrorCode.MALFORMED_REQUEST, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return response(problem(ErrorCode.METHOD_NOT_ALLOWED, null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return response(problem(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null));
    }

    /** Ruta que no existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        return response(problem(ErrorCode.RESOURCE_NOT_FOUND, null));
    }

    /** Cualquier otro error: respuesta genérica; el detalle queda en el registro. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        LOG.error("Error inesperado (traceId={})", traceId(), ex);
        return response(problem(ErrorCode.INTERNAL_ERROR, null));
    }

    // ------------------------------------------------------------------ utilidades

    private ResponseEntity<ProblemDetail> validationResponse(List<FieldViolation> violations) {
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, null);
        problem.setProperty("errors", List.copyOf(violations));
        return response(problem);
    }

    private ProblemDetail problem(ErrorCode code, @Nullable String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(code.httpStatus()), detail == null ? code.defaultMessage() : detail);
        problem.setType(URI.create(code.problemType()));
        problem.setProperty("code", code.name());
        String traceId = traceId();
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
        return problem;
    }

    private ResponseEntity<ProblemDetail> response(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String messageOrDefault(@Nullable String message) {
        return message == null || message.isBlank() ? "El valor no es válido." : message;
    }

    private static @Nullable String traceId() {
        return MDC.get(TRACE_ID_KEY);
    }
}
