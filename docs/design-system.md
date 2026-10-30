# Sistema de diseño de La Viñuela (historia E0-06)

Una sola fuente de verdad para colores, tipografía, espacios y comportamiento de la interfaz. Ninguna pantalla
escribe un color o un tamaño a mano: usa una variable de `frontend/projects/ui/src/styles/_tokens.scss`.

Para verlo en el navegador, abre `docs/design/paleta.html` (claro y oscuro, con el logotipo y el foco de teclado).

## Identidad: el logotipo

El logotipo es el de la tienda: un árbol (encina) junto al agua del embalse, dentro de un anillo amarillo, con
«SUPERMERCADO LA VIÑUELA» en letras verdes. La paleta sale de él.

| Recurso | Uso |
| --- | --- |
| `logo-emblema` (PNG y WebP) | Cabecera de la tienda y del panel, favicon, iconos, tarjetas. Es circular y funciona sobre fondo claro y oscuro |
| `logo-completo` (PNG y WebP) | Portada, pie de página, correos, impresos. **Solo sobre fondo claro**: su borde oscuro se ve mal sobre fondo negro |
| `favicon.ico`, `icon-192.png`, `apple-touch-icon.png` | Pestaña del navegador, instalación en el móvil e iOS |

Reglas de uso:

- Se respetan las proporciones: nunca se estira ni se deforma.
- No se recolorea ni se le ponen sombras o efectos.
- Alrededor del emblema se deja un espacio libre de al menos una cuarta parte de su diámetro.
- Tamaño mínimo del emblema: 32 px. Por debajo de eso, ni se reconoce el árbol ni se lee el anillo.
- El nombre «La Viñuela» junto al emblema se escribe como texto de la interfaz (no como imagen), para que se lea
  bien y lo lean los lectores de pantalla.

> **Importante: la calidad del logotipo.** El archivo recibido es pequeño (288 × 413 píxeles) y con fondo blanco.
> Sirve para la cabecera y el favicon, pero **se verá borroso** en la portada, en pantallas de alta densidad, en
> pantallas grandes y en cualquier impreso. Hay que pedir a quien lo diseñó el original: preferiblemente en
> vectorial (**SVG, PDF o AI**), o como mínimo un PNG de 2000 píxeles o más con fondo transparente. Cuando
> llegue, se sustituyen los ficheros sin cambiar nada más.

## Paleta

| Token | Color | Para qué |
| --- | --- | --- |
| `--brand` | `#007924` (verde del lettering) | Botones principales, enlaces, elementos de marca. Texto blanco encima (5,6:1) |
| `--brand-hover` | `#005F1C` | Botón principal al pasar el ratón |
| `--brand-soft` | `#E5F3E7` | Fondos suaves de marca |
| `--accent` | `#ECDE34` (amarillo del anillo) | Etiquetas de oferta y llamadas a la acción secundarias. **Solo como fondo, con texto oscuro encima**. Nunca como texto ni como foco sobre blanco (1,4:1) |
| `--water` | `#9AD9D1` (agua del embalse) | Fondos decorativos y de apoyo |
| `--earth` | `#6D5D34` (tronco) | Detalles y textos de apoyo |
| `--ink` | `#171716` | Texto principal (17,9:1 sobre blanco) |
| `--muted` | `#566358` | Texto secundario (6,3:1) |
| `--focus` | tinta en claro, amarillo en oscuro | Anillo de foco del teclado, siempre visible |
| `--ok`, `--warn`, `--danger`, `--info` | verde, ámbar, rojo, azul | Avisos, cada uno con su fondo suave |

El **modo oscuro** se activa solo si el sistema del usuario lo pide, o a mano con `data-theme="dark"`. En él el verde
de los textos y enlaces se aclara (`--brand-text`) para mantener el contraste.

### Contraste: se comprueba solo

`node tools/check-contrast.mjs` lee los tokens y verifica **34 pares de color por tema** (claro y oscuro):

- Texto: 4,5:1 como mínimo (WCAG 2.2 AA).
- Foco y bordes de campos de formulario: 3:1 como mínimo.

Si alguien cambia un color y rompe un contraste, el comprobador falla y la integración continua lo detiene.

## Color de marca editable por la empresa

La empresa puede cambiar el color principal desde el panel (historia E10-01). La aplicación aplicará el color
elegido como `--brand` y **recalculará** `--brand-hover`, `--brand-soft`, `--brand-text` y `--on-brand`
comprobando el contraste; si el color elegido no permite texto legible, avisará y propondrá una variante más
oscura. Así nadie puede dejar la tienda ilegible.

## Capas de CSS

```
@layer reset, tokens, base, layout, components, utilities;
```

- `reset`: normalización mínima.  `tokens`: las variables.  `base`: elementos HTML, foco, selección y
  movimiento reducido.
- `layout`: contenedor.  `components`: estilos globales de `ui` (en la parte 2).  `utilities`: unas pocas clases
  de ayuda (`lv-visually-hidden`, `lv-skip-link`).
- Los estilos encapsulados de cada componente de Angular ganan siempre a estas capas dentro de su ámbito.

## Tipografía

Dos familias de licencia abierta (OFL), **autoalojadas** (no se cargan de Google, para no enviar la IP del visitante
a terceros): **Bricolage Grotesque** para títulos y **Figtree** para el texto. Se instalan con los paquetes
`@fontsource-variable/figtree` y `@fontsource-variable/bricolage-grotesque`, y `tools/install-fonts.mjs` las añade
a las dos aplicaciones. Mientras no estén instaladas, el diseño usa la tipografía del sistema.

## Accesibilidad incluida desde ya

- Foco de teclado visible en todo (3 px).
- Objetivo táctil mínimo de 44 px (`--target`).
- Respeto a «reducir movimiento» del sistema.
- Enlace «Saltar al contenido» (`lv-skip-link`).
- Idioma de la página en español.

## Cómo se conecta con las aplicaciones

`tools/apply-brand-assets.mjs` hace que `tienda` y `panel` carguen el sistema de diseño (`@use` de la librería
`ui` en su `styles.scss`) y ajusta su `index.html` (idioma, título, color del navegador, icono de iOS).

## Componentes de la librería `ui`

Todos son *standalone*, con `OnPush`, entradas con signals y prefijo `lv`. Los estilos están en
`frontend/projects/ui/src/styles/_components.scss` (capa `components`) y solo usan tokens.

| Componente | Uso | Accesibilidad |
| --- | --- | --- |
| `lvButton` (directiva) | `<button lvButton="accent" lvSize="large" lvBlock>`. Variantes: `primary` (por defecto), `accent`, `ghost`, `danger`. Vale también para `<a>` | Altura mínima 44 px; foco visible; desactivado con `disabled` |
| `<lv-badge variant="offer">` | Etiquetas: `offer`, `age`, `neutral`, `ok`, `warn`, `danger` | Texto siempre legible (contraste comprobado) |
| `<lv-alert variant="danger">` | Avisos: `ok`, `warn`, `danger`, `info` | `danger` se anuncia al instante (`role="alert"`), el resto con educación (`role="status"`). Siempre con icono: el estado no depende solo del color |
| `<lv-field label hint error required>` + `lvControl` | Campo con etiqueta, ayuda y error sobre `input`, `textarea` o `select` | Etiqueta visible enlazada al control, `aria-describedby`, `aria-invalid`, `aria-required` automáticos; el error se anuncia |
| `<lv-quantity-stepper itemName max [(value)]>` | Selector de cantidad de las tarjetas de producto | Botones con nombre propio («Añadir una unidad de Leche entera»), valor anunciado, límites respetados |
| `<lv-logo name tagline>` | Emblema más nombre | El nombre es texto real; el emblema es decorativo |
| Clases `lv-card`, `lv-price`, `lv-tile--1..6`, `lv-stack`, `lv-row`, `lv-grid-auto` | Tarjeta, precio, marcador de categoría y utilidades de maquetación | — |

Todos tienen pruebas con TestBed (`*.spec.ts`).

### Para verlos

La tienda tiene una página interna con todos los componentes y sus estados, en `/componentes`:

```powershell
cd frontend
npx.cmd ng serve tienda --host 127.0.0.1
```

y abrir `http://127.0.0.1:4200`. Recórrela con la tecla Tab para comprobar el foco.

## Qué falta

Componentes que llegan con las pantallas que los necesitan: diálogo y cajón lateral (carrito), menú, pestañas,
tabla del panel, paginación, notificaciones emergentes (*toast*) y esqueletos de carga. La cabecera y el pie de la
tienda y el menú lateral del panel se construyen con el catálogo y con las primeras pantallas del panel.
