package com.lavinuela.shop.shared.error;

import java.util.Locale;

/**
 * Catálogo de códigos de error de la API (Fase 3, apartado 8.3).
 *
 * <p>Cada código lleva el estado HTTP con el que se responde y un mensaje por defecto, escrito en
 * español claro y sin tecnicismos, que es el que puede ver la persona usuaria. El detalle técnico
 * nunca viaja en la respuesta: va al registro con el identificador de seguimiento.
 */
public enum ErrorCode {

    /** Hay datos que no son correctos. Revísalos e inténtalo de nuevo. */
    VALIDATION_FAILED(400, "Hay datos que no son correctos. Revísalos e inténtalo de nuevo."),
    /** No hemos podido entender la petición. */
    MALFORMED_REQUEST(400, "No hemos podido entender la petición."),
    /** El correo o la contraseña no son correctos. */
    AUTH_INVALID_CREDENTIALS(401, "El correo o la contraseña no son correctos."),
    /** Tu sesión ha caducado. Vuelve a iniciar sesión. */
    AUTH_TOKEN_EXPIRED(401, "Tu sesión ha caducado. Vuelve a iniciar sesión."),
    /** Por seguridad, hemos cerrado tu sesión. Vuelve a iniciar sesión. */
    AUTH_TOKEN_REUSED(401, "Por seguridad, hemos cerrado tu sesión. Vuelve a iniciar sesión."),
    /** No tienes permiso para hacer esto. */
    ACCESS_DENIED(403, "No tienes permiso para hacer esto."),
    /** No hemos encontrado lo que buscas. */
    RESOURCE_NOT_FOUND(404, "No hemos encontrado lo que buscas."),
    /** Esta operación no está permitida. */
    METHOD_NOT_ALLOWED(405, "Esta operación no está permitida."),
    /** El formato de los datos enviados no es compatible. */
    UNSUPPORTED_MEDIA_TYPE(415, "El formato de los datos enviados no es compatible."),
    /** Algún producto ya no tiene existencias suficientes. */
    STOCK_INSUFFICIENT(409, "Algún producto ya no tiene existencias suficientes."),
    /** Ha cambiado el precio de algún producto de tu carrito. */
    CART_PRICE_CHANGED(409, "Ha cambiado el precio de algún producto de tu carrito."),
    /** Otra persona ha cambiado esto mientras lo editabas. Recarga la página y vuelve a intentarlo. */
    CONCURRENT_MODIFICATION(409, "Otra persona ha cambiado esto mientras lo editabas. Recarga la página y vuelve a intentarlo."),
    /** El pedido no puede pasar a ese estado ahora mismo. */
    ORDER_INVALID_TRANSITION(409, "El pedido no puede pasar a ese estado ahora mismo."),
    /** Ese nombre de usuario ya está en uso. Elige otro. */
    USERNAME_TAKEN(409, "Ese nombre de usuario ya está en uso. Elige otro."),

    /** El código no es correcto. Revísalo e inténtalo de nuevo. */
    VERIFICATION_CODE_INVALID(422, "El código no es correcto. Revísalo e inténtalo de nuevo."),

    /** El código ha caducado. Pide uno nuevo. */
    VERIFICATION_CODE_EXPIRED(422, "El código ha caducado. Pide uno nuevo."),

    /** Has probado demasiados códigos. Pide uno nuevo. */
    VERIFICATION_TOO_MANY_ATTEMPTS(429, "Has probado demasiados códigos. Pide uno nuevo."),

    /** Este pedido ya no se puede cancelar. */
    ORDER_NOT_CANCELLABLE(409, "Este pedido ya no se puede cancelar."),
    /** Esta dirección está fuera de nuestra zona de reparto. */
    DELIVERY_OUT_OF_ZONE(422, "Esta dirección está fuera de nuestra zona de reparto."),
    /** No hemos podido localizar esa dirección. */
    DELIVERY_ADDRESS_NOT_FOUND(422, "No hemos podido localizar esa dirección."),
    /** El pedido no llega al importe mínimo. */
    ORDER_MIN_AMOUNT(422, "El pedido no llega al importe mínimo."),
    /** Tienes que confirmar que eres mayor de 18 años para comprar estos productos. */
    AGE_CONFIRMATION_REQUIRED(422, "Tienes que confirmar que eres mayor de 18 años para comprar estos productos."),
    /** El importe del pago no coincide con el del pedido. */
    PAYMENT_AMOUNT_MISMATCH(422, "El importe del pago no coincide con el del pedido."),
    /** No se puede hacer este reembolso. */
    REFUND_NOT_ALLOWED(422, "No se puede hacer este reembolso."),
    /** El cupón no es válido. */
    COUPON_INVALID(422, "El cupón no es válido."),
    /** Esta petición ya se envió con otros datos. */
    IDEMPOTENCY_CONFLICT(422, "Esta petición ya se envió con otros datos."),
    /** El fichero de importación no es válido. */
    IMPORT_INVALID_FILE(422, "El fichero de importación no es válido."),
    /** Tu cuenta está bloqueada temporalmente por demasiados intentos. Inténtalo más tarde. */
    AUTH_ACCOUNT_LOCKED(423, "Tu cuenta está bloqueada temporalmente por demasiados intentos. Inténtalo más tarde."),
    /** Has hecho demasiadas peticiones. Espera un momento e inténtalo de nuevo. */
    RATE_LIMITED(429, "Has hecho demasiadas peticiones. Espera un momento e inténtalo de nuevo."),
    /** Ha ocurrido un error inesperado. Inténtalo de nuevo en unos minutos. */
    INTERNAL_ERROR(500, "Ha ocurrido un error inesperado. Inténtalo de nuevo en unos minutos."),
    /** No hemos podido comunicarnos con el banco. Inténtalo de nuevo. */
    PAYMENT_GATEWAY_ERROR(502, "No hemos podido comunicarnos con el banco. Inténtalo de nuevo."),
    /** No podemos calcular el reparto ahora mismo. Inténtalo de nuevo en unos minutos. */
    DELIVERY_SERVICE_UNAVAILABLE(503, "No podemos calcular el reparto ahora mismo. Inténtalo de nuevo en unos minutos."),

    /** No hemos podido enviar el correo ahora mismo. */
    EMAIL_SERVICE_UNAVAILABLE(503, "No hemos podido enviar el correo ahora mismo. Inténtalo de nuevo en unos minutos.");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /** Estado HTTP con el que se responde. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Mensaje por defecto para la persona usuaria. */
    public String defaultMessage() {
        return defaultMessage;
    }

    /** Identificador del tipo de problema (campo {@code type} de RFC 9457), por ejemplo {@code urn:lavinuela:problem:stock-insufficient}. */
    public String problemType() {
        return "urn:lavinuela:problem:" + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
