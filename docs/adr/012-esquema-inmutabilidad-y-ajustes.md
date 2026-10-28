# ADR-012: Inmutabilidad por triggers y ajustes del esquema respecto a la Fase 5

**Estado:** Aceptada

## Contexto

Al convertir el diseño de la Fase 5 en migraciones reales aparecieron tres problemas: (1) una tabla de
facturas con un campo que se rellena después (el PDF) no puede ser realmente inmutable; (2) los permisos por
tabla no se pueden aplicar sobre tablas que todavía no existen cuando se crean los usuarios, ni se pueden
restar de un permiso de esquema; (3) MySQL no permite acciones en cascada sobre columnas base de columnas
calculadas almacenadas.

## Decisión

- La inmutabilidad de auditoría, eventos de pago, movimientos de stock, cronología de pedidos, historial de
  precios y facturas se garantiza con *triggers* de una sola sentencia (V010), válidos para cualquier usuario.
- El PDF de la factura se guarda en una tabla aparte, de solo inserción.
- Las columnas calculadas con índice único son `VIRTUAL`, y no se usa `ON DELETE CASCADE` donde la columna
  base participa en ellas.
- Los permisos finos por tabla para producción se preparan en la historia E15-06.

## Consecuencias

La garantía de inmutabilidad no depende de cómo se configuren los usuarios. Hay que asegurar que el servidor
de producción permite crear *triggers* con el registro binario activado (`log_bin_trust_function_creators=1`
o equivalente), algo que se confirma al elegir alojamiento. Detalle en `docs/database.md`.
