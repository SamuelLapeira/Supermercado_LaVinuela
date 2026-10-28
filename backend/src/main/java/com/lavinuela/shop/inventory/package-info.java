/**
 * Inventario.
 *
 * <p>Existencias, movimientos de stock y reservas con caducidad. Garantiza que nunca se vende lo que no hay.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.inventory.InventoryApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Inventario", allowedDependencies = {"catalog"})
@NullMarked
package com.lavinuela.shop.inventory;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
