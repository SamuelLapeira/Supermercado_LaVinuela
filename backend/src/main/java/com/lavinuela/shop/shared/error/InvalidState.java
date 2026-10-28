package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * El recurso no está en un estado que permita la operación (por ejemplo, cancelar un pedido ya en preparación).
 *
 * <p>Se responde, según su código, con un estado HTTP como 409.
 */
public final class InvalidState extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidState(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public InvalidState(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public InvalidState(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
