/**
 * Auditoría.
 *
 * <p>Registro inmutable de acciones del personal y eventos de seguridad. Módulo compartido: lo usan todos los demás.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.audit.AuditApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Auditoría", allowedDependencies = {})
@NullMarked
package com.lavinuela.shop.audit;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
