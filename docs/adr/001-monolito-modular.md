# ADR-001: Monolito modular, no microservicios

**Estado:** Aceptada

## Contexto

Equipo reducido, un solo negocio y transacciones fuertes entre pedido, stock y pago.

## Decisión

Un único artefacto desplegable con módulos por paquete. Las fronteras se imponen con Spring Modulith y ArchUnit en cada compilación.

## Consecuencias

Un despliegue simple y coherencia transaccional. Si un módulo tuviera que separarse algún día, las fronteras ya están marcadas.
