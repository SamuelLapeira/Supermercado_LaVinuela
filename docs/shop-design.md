# Diseño de la tienda: maqueta navegable

La tienda se está diseñando **con pantallas que ya se pueden recorrer en el navegador**, con datos de ejemplo. Así se ve y se
puede enseñar el resultado desde el primer día, y las mismas pantallas se conectarán después al servidor sin rehacerlas.

> **Todo lo que se ve son datos de ejemplo:** productos, marcas, precios, fotos (de momento un icono por producto) y
> condiciones de reparto. Una franja amarilla en la cabecera lo avisa en todas las pantallas.

## Idea de diseño

Tomamos de las tiendas online de supermercado que funcionan lo que ayuda a comprar (buscador siempre a mano, categorías a la
vista, botón de añadir evidente, aviso de pedido mínimo y de envío gratis) y le ponemos **identidad propia**: la paleta del
logotipo (verde, amarillo, agua y tierra), el orgullo de producto de la sierra y un tono cercano.

| Elemento | Decisión |
| --- | --- |
| Botón principal de compra | **Amarillo** del logotipo («Añadir»); el verde queda para navegar y confirmar |
| Producto local | Etiqueta verde «De la sierra» y sección propia en la portada |
| Categorías | 8, con subcategorías, cada una con su color |
| Fotos | Provisionalmente un icono por producto; el diseño está preparado para fotos propias (nunca logos de marcas) |
| Móvil | Productos de dos en dos, buscador en segunda fila, categorías desplazables, panel de cesta a pantalla completa |
| Accesibilidad | Contraste AA, foco visible, objetivos táctiles de 44 px, lectores de pantalla (la cantidad y el progreso se anuncian) |

## Mapa de pantallas

| Dirección | Pantalla | Estado |
| --- | --- | --- |
| `/` | Portada: ventajas, categorías, ofertas, producto local, cómo funciona, tienda física | Hecha |
| `/categoria/:slug` | Listado de una categoría con filtros (subcategoría, etiquetas, marcas) y orden | Hecha |
| `/buscar?q=` | Resultados de búsqueda (sin acentos, varias palabras) | Hecha |
| `/ofertas` | Ofertas de la semana | Hecha |
| `/producto/:slug` | Ficha: precio, cantidad, alérgenos, nutrición, productos parecidos | Hecha |
| `/cesta` | Cesta con cálculo de envío por localidad, pedido mínimo y envío gratis | Hecha |
| (panel lateral) | Cesta rápida desde la cabecera | Hecha |
| `/pedido` | Dirección, pago y confirmación | **Siguiente entrega** |
| `/cuenta/pedidos` y detalle | Historial y seguimiento de pedidos | **Siguiente entrega** |
| `/cuenta` | Perfil, direcciones, datos | **Siguiente entrega** (hoy solo saluda) |
| `/entrar`, `/registro`, `/verificar` | Cuenta | Hecha (funciona de verdad) |

## Qué funciona ya y qué no

**Funciona (en el navegador, sin servidor):** buscar, filtrar, ordenar, añadir a la cesta, cambiar cantidades, el panel de la cesta,
el cálculo de totales y de envío, el aviso de pedido mínimo, de envío gratis y de «fuera de zona», y la advertencia de
productos para mayores de 18 años. **La cesta se recuerda entre visitas** (solo en este navegador).

**No funciona todavía:** el pago, los pedidos y los datos de la cuenta. Los productos y precios no vienen del servidor.

## Datos de ejemplo que hay que confirmar con la empresa

Estos valores están en `features/cart/delivery-demo.ts` y son **inventados para la maqueta**:

| Dato | Valor de ejemplo |
| --- | --- |
| Pedido mínimo | 20 € |
| Envío gratis desde | 60 € |
| Distancia máxima de reparto | 40 km por carretera |
| Tarifas | hasta 10 km: 3 €; hasta 20 km: 4,50 €; hasta 30 km: 6 €; hasta 40 km: 8 € |
| Plazo de entrega | 1 a 3 días |
| Horario de la tienda | L-S 9:00 a 21:00; domingos y festivos 9:30 a 14:30 |

Cuando se confirmen, vivirán en los ajustes de la tienda (editables desde el panel de gestión).

## Cómo está hecho

- **Catálogo de ejemplo:** `features/catalog/catalog.data.ts` (48 productos en 8 categorías). Cuando exista el servidor, ese
  fichero desaparece y `CatalogService` leerá de la API: las pantallas no cambian.
- **Filtros y búsqueda:** `catalog-filter.ts`, funciones puras con pruebas. Los filtros y el orden viven **en la dirección web**
  (`?sub=leche&marcas=...&etiquetas=local&orden=precio-asc`), así que se pueden compartir y el botón «atrás» funciona.
- **Cesta:** `features/cart/cart.store.ts` (todos los importes redondeados a céntimos) y `delivery-pricing.ts` (tarifas).
- **Sistema de diseño:** se han añadido `lv-progress` (barra de progreso) y la clase `lv-chip` (botones de filtro).

## Cómo verlo

Con la tienda arrancada (`npx.cmd ng serve tienda --host 127.0.0.1`), abre `http://127.0.0.1:4200`. No hace falta el servidor para
recorrer el catálogo y la cesta; sí para entrar o registrarse.
