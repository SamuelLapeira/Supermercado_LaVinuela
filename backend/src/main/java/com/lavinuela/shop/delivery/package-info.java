/**
 * Reparto.
 *
 * <p>Geocodificación, distancia por carretera, tramos de tarifa, plazo estimado y política de reparto.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.delivery.DeliveryApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Reparto", allowedDependencies = {"content"})
@NullMarked
package com.lavinuela.shop.delivery;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
