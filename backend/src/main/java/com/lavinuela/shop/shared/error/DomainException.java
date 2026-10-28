package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * Una regla de negocio no se cumple. Es un resultado esperable, no un fallo del sistema.
 *
 * <p>Sus subclases son {@link BusinessRuleViolation}, {@link ResourceNotFound}, {@link ConflictException},
 * {@link ForbiddenOperation}, {@link InvalidState} y {@link ValidationFailedException}.
 */
public abstract class DomainException extends CodedException {

    private static final long serialVersionUID = 1L;

    protected DomainException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
