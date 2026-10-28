# ADR-011: Dependencias entre módulos (ajuste sobre la Fase 3, apartado 1.2)

**Estado:** Aceptada

## Contexto

Al convertir la tabla de dependencias de la Fase 3 en reglas ejecutables apareció un ciclo: pedidos dependía de pagos, y pagos dependía de pedidos. Además, inventario escuchaba eventos de pedidos mientras pedidos llamaba a inventario.

## Decisión

(1) Pedidos NO depende de pagos: pagos implementa un puerto que define pedidos (inversión de dependencias) y es pagos quien depende de pedidos. (2) Pedidos llama directamente a inventario para reservar, confirmar y liberar stock dentro de su transacción; inventario no escucha eventos de pedidos. (3) Los eventos (OrderPaid, OrderStatusChanged...) quedan para consumidores no críticos: notificaciones, facturación e informes, que dependen de pedidos y no al revés. (4) Identidad depende de reparto (calcula la distancia al guardar una dirección) y reparto de contenido (lee su configuración). (5) El módulo audit es compartido: lo usan todos y no usa a ninguno. Las dependencias permitidas están declaradas en el package-info.java de cada módulo y las comprueban ModularityTests y ArchitectureTests.

## Consecuencias

El grafo de dependencias queda sin ciclos. Si una historia futura necesita una dependencia nueva, se discute y se registra aquí antes de añadirla.
