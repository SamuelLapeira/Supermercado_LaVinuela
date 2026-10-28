package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * Un servicio externo no responde (por ejemplo, el de rutas).
 *
 * <p>Se responde, según su código, con un estado HTTP como 503.
 */
public final class ExternalServiceUnavailable extends InfrastructureException {

    private static final long serialVersionUID = 1L;

    public ExternalServiceUnavailable(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public ExternalServiceUnavailable(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public ExternalServiceUnavailable(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
