/**
 * Contenido y configuración.
 *
 * <p>Ajustes del sitio y del negocio, banner, páginas, plantillas de correo, medios e importaciones: lo que la empresa gestiona sin programadores.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.content.ContentApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Contenido y configuración", allowedDependencies = {})
@NullMarked
package com.lavinuela.shop.content;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
