# Base de datos (historia E0-03)

MySQL 8.4 LTS, motor InnoDB, esquema controlado **solo** con Flyway. La aplicación arranca con
`ddl-auto=validate`: si las entidades no coinciden con las tablas, no arranca.

El diseño completo está en la Fase 5. Este documento recoge cómo se ha materializado y en qué se ha
desviado, con el motivo.

## Migraciones

Están en `backend/src/main/resources/db/migration`.

| Fichero | Contenido |
| --- | --- |
| `V001__identity.sql` | Usuarios, consentimientos, roles y permisos, segundo factor, tokens y direcciones |
| `V002__media_catalog.sql` | Medios (con origen y derechos), categorías, marcas, IVA, productos, códigos de barras, imágenes, alérgenos, historial de precios |
| `V003__inventory.sql` | Existencias, reservas con caducidad y movimientos |
| `V004__promotions_cart_delivery_settings.sql` | Cupones, tramos de tarifa, ajustes y carrito |
| `V005__ordering.sql` | Pedidos, líneas, cronología, incidencias, secuencias y canjes de cupón |
| `V006__payments_billing.sql` | Pagos, eventos de Redsys, reembolsos, facturas y sus líneas y documentos |
| `V007__content_notifications_audit.sql` | Bloques y páginas de contenido, plantillas de correo, importaciones, notificaciones y auditoría |
| `V008__system_and_reporting_tables.sql` | Idempotencia, bloqueo de tareas y tablas resumen para informes |
| `V009__fulltext_search.sql` | Índice de búsqueda de texto con *ngram* |
| `V010__immutability_triggers.sql` | Registros que no se pueden modificar ni borrar |
| `V100__seed_reference_data.sql` | Roles, permisos, IVA, alérgenos, tarifas, ajustes, banner, páginas legales (borrador) y plantillas de correo |
| `V101__brand_from_logo.sql` | El color principal por defecto pasa a ser el verde del logotipo (`#007924`) |
| `V102__registration_with_verification_code.sql` | Registro con código: nombre y apellidos opcionales, `users.username` único, intentos fallidos en los códigos y plantillas de correo con código |

Resultado: **53 tablas** de negocio más la de historial de Flyway.

### Reglas para nuevas migraciones

1. **Nunca se edita una migración ya aplicada** en un entorno compartido: se añade una nueva.
2. Cambios en producción con el patrón *expand/contract*: primero se añade (compatible con el código
   antiguo), después se despliega y más tarde se elimina lo obsoleto.
3. No se usan `${...}` en el SQL: Flyway los interpreta como marcadores.
4. Todo `FOREIGN KEY` se declara a nivel de tabla (MySQL ignora el `REFERENCES` escrito en la columna) y
   lleva un índice que empiece por su columna.
5. Los *triggers* son de una sola sentencia, sin bloques `BEGIN ... END`.

## Convenciones

| Tema | Decisión |
| --- | --- |
| Claves | `BIGINT UNSIGNED` internas; `public_id CHAR(26)` (ULID) para lo que sale por la API |
| Fechas | `DATETIME(3)` en UTC; fechas sueltas como `DATE` |
| Dinero | `DECIMAL(10,2)`. Cantidades de stock y de línea: enteros |
| Estados | `VARCHAR` más `CHECK`; sin `ENUM` |
| Borrado | Sin borrado físico en catálogo, pedidos, pagos ni facturas |
| Colación | `utf8mb4_0900_ai_ci`: sin distinguir mayúsculas ni acentos |
| Aislamiento | `READ COMMITTED` (menos bloqueos de hueco en las actualizaciones de stock) |

## Garantías que da la propia base de datos

Aunque la aplicación falle, la base de datos impide:

- Stock negativo, o más reservado que existente.
- Dos pagos cobrados para el mismo pedido.
- Dos direcciones por defecto o dos carritos activos para el mismo usuario.
- Un pedido cuyo total no cuadra con sus importes, o una línea cuyo total no es precio por cantidad.
- Una referencia de Redsys con formato incorrecto.
- Modificar o borrar auditoría, eventos de pago, movimientos de stock, cronología de pedidos, historial de
  precios, facturas, líneas de factura y documentos de factura.

Todo esto está comprobado por `DatabaseMigrationTests`.

## Desviaciones respecto a la Fase 5

| # | Desviación | Motivo |
| --- | --- | --- |
| 1 | **El PDF de la factura va en una tabla aparte** (`invoice_documents`, solo inserción) y `invoices` no tiene `pdf_storage_key` | Así la factura emitida es completamente inmutable, sin excepciones que obliguen a un trigger complejo |
| 2 | **La inmutabilidad se garantiza con *triggers*** (de una sola sentencia) y no solo con permisos | Vale para cualquier usuario, incluido quien acceda a mano, y no exige permisos por tabla que no se pueden aplicar con permisos de esquema. Los permisos finos por tabla para producción se prepararán en la historia E15-06 |
| 3 | Columnas calculadas **`VIRTUAL`** (no `STORED`) cuando llevan un índice único, y **sin `ON DELETE CASCADE`** en `product_images.product_id` | MySQL no admite acciones referenciales en cascada sobre la columna base de una columna calculada almacenada. Los productos nunca se borran, así que no se pierde nada |
| 4 | `coupons` y `coupon_scopes` se crean en V004 (antes del carrito) y `coupon_redemptions` en V005 | La clave foránea del carrito necesita que los cupones existan antes |
| 5 | La tabla de eventos de Spring Modulith (`event_publication`) **no está** en las migraciones | Su esquema depende de la versión exacta; se añadirá con la historia que incorpore el registro de eventos, copiándolo de la versión en uso |
| 6 | Permiso nuevo `REPORTS_STOCK_READ` | El encargado de catálogo solo ve informes de stock (Fase 3, apartado 9.3) |
| 7 | Las coordenadas de la tienda sembradas son **aproximadas** (las del municipio) | Hay que sustituirlas por las exactas desde el panel o en una migración |

## Pruebas

`DatabaseMigrationTests` y `ProductSearchTests` arrancan un MySQL 8.4 real en un contenedor temporal
(Testcontainers) y necesitan Docker. Sin Docker se **saltan** (no fallan); en integración continua siempre
deben ejecutarse.

`ProductSearchTests` comprueba, entre otras cosas, que la búsqueda ignora acentos y tolera fragmentos de
palabra. Si alguna falla, es información útil sobre la relevancia de la búsqueda y se valora el plan B.
