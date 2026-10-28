# ADR-004: Estado del frontend con signals

**Estado:** Aceptada

## Contexto

A esta escala una librería de estado global añade ceremonia sin beneficio.

## Decisión

Stores sencillos con signals; el servidor es siempre la autoridad en precios, totales y stock.

## Consecuencias

Menos dependencias. Se reconsidera si el estado compartido se complica.
