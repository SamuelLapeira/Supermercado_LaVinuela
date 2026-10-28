/**
 * Pagos.
 *
 * <p>Pasarela Redsys, pagos, notificaciones firmadas, conciliación y reembolsos. Depende de pedidos, no al revés.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.payments.PaymentsApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Pagos", allowedDependencies = {"ordering"})
@NullMarked
package com.lavinuela.shop.payments;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
