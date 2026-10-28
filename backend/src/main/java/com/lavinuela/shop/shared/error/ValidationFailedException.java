package com.lavinuela.shop.shared.error;

import java.util.List;
import java.util.Map;

/**
 * Los datos recibidos no son válidos. Lleva la lista de errores por campo.
 *
 * <p>Se responde con 400 y el detalle en el campo {@code errors} de la respuesta.
 */
public final class ValidationFailedException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final transient List<FieldViolation> violations;

    public ValidationFailedException(List<FieldViolation> violations) {
        super(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), Map.of());
        this.violations = List.copyOf(violations);
    }

    /** Un único error de campo. */
    public static ValidationFailedException of(String field, String message) {
        return new ValidationFailedException(List.of(FieldViolation.of(field, message)));
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
