# ADR-008: Redsys por redirección; la notificación del servidor es la fuente de verdad

**Estado:** Aceptada

## Contexto

Mantener la aplicación fuera del alcance PCI y evitar depender del navegador del cliente.

## Decisión

Formulario firmado y redirección. Notificación servidor a servidor idempotente. El retorno del navegador solo consulta el estado real.

## Consecuencias

Necesita conciliación periódica y tratamiento de pagos tardíos.
