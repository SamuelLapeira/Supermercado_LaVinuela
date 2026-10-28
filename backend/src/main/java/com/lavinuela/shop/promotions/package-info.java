/**
 * Promociones.
 *
 * <p>Cupones y promociones (versión 1.1; las tablas existen desde el MVP).
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.promotions.PromotionsApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Promociones", allowedDependencies = {"catalog"})
@NullMarked
package com.lavinuela.shop.promotions;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
