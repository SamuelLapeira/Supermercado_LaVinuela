package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * Falla algo externo a la aplicación (servicio de rutas, banco, correo...).
 *
 * <p>Sus subclases son {@link ExternalServiceUnavailable} y {@link PaymentGatewayException}.
 */
public abstract class InfrastructureException extends CodedException {

    private static final long serialVersionUID = 1L;

    protected InfrastructureException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
