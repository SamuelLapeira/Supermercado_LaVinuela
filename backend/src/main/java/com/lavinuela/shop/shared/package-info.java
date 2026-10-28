/**
 * Núcleo compartido.
 *
 * <p>Piezas mínimas y sin lógica de negocio que usan todos los módulos: dinero, identificadores públicos,
 * reloj, errores base y paginación (historia E0-04). No depende de ningún módulo.
 */
@ApplicationModule(displayName = "Núcleo compartido", type = ApplicationModule.Type.OPEN)
@NullMarked
package com.lavinuela.shop.shared;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
