/**
 * Identidad.
 *
 * <p>Usuarios, roles y permisos, sesiones (tokens de acceso y de refresco), direcciones de entrega y consentimientos.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.identity.IdentityApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Identidad", allowedDependencies = {"delivery"})
@NullMarked
package com.lavinuela.shop.identity;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
