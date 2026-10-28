package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * La persona autenticada no tiene permiso para esta operación.
 *
 * <p>Se responde, según su código, con un estado HTTP como 403.
 */
public final class ForbiddenOperation extends DomainException {

    private static final long serialVersionUID = 1L;

    public ForbiddenOperation(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public ForbiddenOperation(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public ForbiddenOperation(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
