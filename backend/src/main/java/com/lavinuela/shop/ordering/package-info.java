/**
 * Pedidos.
 *
 * <p>Checkout, pedido y su máquina de estados, instantáneas de precios y dirección, incidencias por error de la tienda.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.ordering.OrderingApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Pedidos", allowedDependencies = {"identity", "cart", "catalog", "inventory", "delivery", "promotions", "content"})
@NullMarked
package com.lavinuela.shop.ordering;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
