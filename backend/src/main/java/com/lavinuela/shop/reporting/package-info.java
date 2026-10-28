/**
 * Informes.
 *
 * <p>Paneles e informes de solo lectura sobre tablas resumen.
 *
 * <p>La API pública del módulo es {@link com.lavinuela.shop.reporting.ReportingApi} y los eventos que publica.
 * Los subpaquetes {@code domain}, {@code application} e {@code infrastructure} son internos.
 */
@ApplicationModule(displayName = "Informes", allowedDependencies = {"ordering", "catalog", "inventory"})
@NullMarked
package com.lavinuela.shop.reporting;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
