/**
 * Facturación.
 *
 * <p>Tickets y facturas con numeración correlativa, inmutables una vez emitidos.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.billing.BillingApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Facturación", allowedDependencies = {"ordering", "payments"})
@NullMarked
package com.lavinuela.shop.billing;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
