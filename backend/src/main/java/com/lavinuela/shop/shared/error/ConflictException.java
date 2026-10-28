package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * La operación choca con el estado actual (stock agotado, edición concurrente, duplicado).
 *
 * <p>Se responde, según su código, con un estado HTTP como 409.
 */
public final class ConflictException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ConflictException(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public ConflictException(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public ConflictException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
