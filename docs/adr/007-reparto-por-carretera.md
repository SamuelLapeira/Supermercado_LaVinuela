# ADR-007: Reparto por distancia por carretera con anillos de tarifa

**Estado:** Aceptada

## Contexto

El negocio reparte hasta una distancia máxima medida por carretera, no en línea recta.

## Decisión

Puertos Geocoder y RoutingService con caché y estimación de reserva; distancia y tarifa guardadas en el pedido.

## Consecuencias

Dependencia de un proveedor de rutas, aún por elegir.
