/**
 * Notificaciones.
 *
 * <p>Envío de correos transaccionales con registro y reintentos.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.notifications.NotificationsApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Notificaciones", allowedDependencies = {"ordering", "identity", "payments", "inventory", "content"})
@NullMarked
package com.lavinuela.shop.notifications;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
