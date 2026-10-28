# ADR-013: Identificador de petición propio y puerto de gestión aparte

**Estado:** Aceptada

## Contexto

Hace falta poder seguir un problema desde que lo ve la persona usuaria hasta el registro del servidor, y
vigilar la salud y las métricas sin exponerlas al público. Aún no se ha decidido el alojamiento ni el sistema
de métricas y registros.

## Decisión

- Un filtro propio da a cada petición un identificador (`X-Request-Id`, que se acepta solo si es válido), lo
  añade a todos los registros y lo devuelve en la respuesta; las respuestas de error lo llevan como `traceId`.
  Cuando se incorpore OpenTelemetry, el identificador de traza ocupará su lugar.
- Las sondas de salud, las métricas (formato Prometheus, neutro respecto al proveedor) y la información de
  compilación van en un **puerto de gestión aparte**, solo accesible desde la propia máquina por defecto.
- Las sondas no revelan detalles: solo `UP` o `DOWN`. La de disponibilidad (*readiness*) incluye la base de datos.
- En producción los registros salen en JSON (formato ECS); en local, en texto legible.
- Los datos personales solo se escriben enmascarados.

## Consecuencias

La observabilidad básica funciona sin depender del alojamiento. Al elegirlo habrá que decidir qué sistema
recoge las métricas y los registros, y que el puerto de gestión solo sea alcanzable desde la red interna.
Detalle en `docs/observability.md`.
