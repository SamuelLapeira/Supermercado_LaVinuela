# ADR-002: Pila tecnológica y versiones

**Estado:** Aceptada

## Contexto

Se pidieron versiones seguras y sin APIs deprecadas. Spring Boot 3.5 dejó de tener soporte abierto el 30-jun-2026 y Angular 20 termina en noviembre de 2026.

## Decisión

Java 25 LTS, Spring Boot 4.1.x, Angular 22, MySQL 8.4 LTS. Compilación con -Xlint:all -Werror: una deprecación rompe el build.

## Consecuencias

Hay que verificar cada librería adicional contra Spring Boot 4.1 y Angular 22 antes de añadirla. Calendario fijo de actualizaciones (parches mensuales, versiones mayores cada 6 meses en Angular).
