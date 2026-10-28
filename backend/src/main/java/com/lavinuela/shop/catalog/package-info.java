/**
 * Catálogo.
 *
 * <p>Productos, categorías, marcas, tipos de IVA, imágenes, precios e historial de precios, y búsqueda.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.catalog.CatalogApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Catálogo", allowedDependencies = {"content"})
@NullMarked
package com.lavinuela.shop.catalog;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
