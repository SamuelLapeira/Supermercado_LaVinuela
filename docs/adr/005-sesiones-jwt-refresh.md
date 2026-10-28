# ADR-005: Sesiones: JWT corto y refresh rotatorio

**Estado:** Aceptada

## Contexto

Seguridad de sesiones y futura app móvil con la misma API.

## Decisión

Token de acceso de 10-15 min en memoria; refresh opaco de un solo uso en cookie HttpOnly con detección de reutilización. Nunca en localStorage.

## Consecuencias

Mayor complejidad en el cliente (cola única de renovación), a cambio de una superficie de ataque menor.
