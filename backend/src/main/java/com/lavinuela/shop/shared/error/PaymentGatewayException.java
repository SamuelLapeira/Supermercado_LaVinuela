package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * La pasarela de pago ha fallado o ha respondido algo inesperado.
 *
 * <p>Se responde, según su código, con un estado HTTP como 502.
 */
public final class PaymentGatewayException extends InfrastructureException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayException(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public PaymentGatewayException(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public PaymentGatewayException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
