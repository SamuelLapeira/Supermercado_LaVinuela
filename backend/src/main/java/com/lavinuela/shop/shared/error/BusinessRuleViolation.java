package com.lavinuela.shop.shared.error;

import java.util.Map;

/**
 * Se incumple una regla de negocio (por ejemplo, el pedido no llega al mínimo).
 *
 * <p>Se responde, según su código, con un estado HTTP como 422.
 */
public final class BusinessRuleViolation extends DomainException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleViolation(ErrorCode code) {
        super(code, code.defaultMessage(), Map.of());
    }

    public BusinessRuleViolation(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public BusinessRuleViolation(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
