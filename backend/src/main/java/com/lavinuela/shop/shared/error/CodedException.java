package com.lavinuela.shop.shared.error;

import java.util.Map;
import java.util.Objects;

/**
 * Raíz de las excepciones de la aplicación: todas llevan un {@link ErrorCode}.
 *
 * <p>El controlador global de errores las convierte en respuestas {@code problem+json}. Se dividen en
 * {@link DomainException} (una regla de negocio no se cumple) e {@link InfrastructureException} (falla
 * algo externo: rutas, banco, correo).
 */
public abstract class CodedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final transient Map<String, Object> details;

    protected CodedException(ErrorCode code, String message, Map<String, Object> details) {
        super(Objects.requireNonNull(message, "El mensaje es obligatorio"));
        this.code = Objects.requireNonNull(code, "El código de error es obligatorio");
        this.details = Map.copyOf(details);
    }

    public ErrorCode code() {
        return code;
    }

    /** Datos adicionales que se pueden mostrar al cliente (por ejemplo, los productos sin stock). */
    public Map<String, Object> details() {
        return details;
    }
}
