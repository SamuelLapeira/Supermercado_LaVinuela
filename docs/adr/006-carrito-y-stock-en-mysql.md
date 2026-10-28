# ADR-006: Carrito y stock en MySQL; Redis solo como caché

**Estado:** Aceptada

## Contexto

Redis puede vaciarse o caerse y no debe perder datos de negocio.

## Decisión

Fuente de verdad en MySQL. Redis para caché, límites de ritmo y claves temporales. Si Redis cae, la tienda sigue funcionando más lenta.

## Consecuencias

Cada lectura cacheada necesita su invalidación por evento.
