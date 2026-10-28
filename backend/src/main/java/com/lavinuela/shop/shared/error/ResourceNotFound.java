package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * El recurso no existe o no es de quien lo pide. En los dos casos se responde igual para no revelar si existe.
 *
 * <p>Se responde, según su código, con un estado HTTP como 404.
 */
public final class ResourceNotFound extends DomainException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFound(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public ResourceNotFound(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public ResourceNotFound(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
