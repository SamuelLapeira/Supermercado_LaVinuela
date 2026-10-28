/**
 * Carrito.
 *
 * <p>Carrito persistente de visitantes y clientes, con avisos de cambios de precio y de disponibilidad.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.cart.CartApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Carrito", allowedDependencies = {"catalog", "inventory", "promotions"})
@NullMarked
package com.lavinuela.shop.cart;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
