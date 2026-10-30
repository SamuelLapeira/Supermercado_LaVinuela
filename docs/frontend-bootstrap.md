# Frontend: workspace de Angular (historia E0-01)

El workspace **no se escribe a mano**: se crea con el generador oficial de Angular 22, para que
`angular.json` y el resto de ficheros coincidan exactamente con esa versión.

Angular 22 exige **Node.js 22.22.3 o superior, 24.15 o superior, o 26**.

## Estado

Creado y compilando (28-oct-2026): aplicaciones `tienda` y `panel`, librerías `ui`, `core`, `api` y `util`,
TypeScript y plantillas en modo estricto, límites de tamaño de producción y ESLint con las reglas del proyecto.
El paquete inicial de cada aplicación vacía ocupa unos 217 kB sin comprimir (unos 60 kB comprimidos).

## Volver a crearlo (si hiciera falta)

```powershell
.\tools\bootstrap-frontend.ps1        # Windows
./tools/bootstrap-frontend.sh          # Linux o Mac
cd frontend
npx.cmd ng add angular-eslint --skip-confirmation
node ..\tools\setup-frontend-lint.mjs
npx.cmd ng lint
npx.cmd ng build tienda
npx.cmd ng build panel
```

## ESLint y fronteras entre proyectos

`tools/setup-frontend-lint.mjs` escribe `eslint.config.mjs`, añade un destino `lint` a cada proyecto de
`angular.json` y el script `npm run lint`. `ng add angular-eslint` solo instala los paquetes: en un workspace
sin aplicación raíz no crea la configuración.

Reglas que hace cumplir:

| Regla | Qué impide |
| --- | --- |
| **Fronteras** (`no-restricted-imports`) | `ui` solo puede usar `util`; `core`, `api` y `util` según el esquema de abajo; `tienda` y `panel` no se importan entre sí |
| Prefijo `lv` | Todo componente y directiva se llama `lv-...` |
| Sin `any` | Tipado estricto |
| Sin `bypassSecurityTrust*` | Nunca se salta la protección de Angular contra XSS |
| Sin `localStorage` ni `sessionStorage` | El token vive solo en memoria; una excepción inocua se justifica con un comentario |
| Sin `console.log` | Solo `warn` y `error` |
| Accesibilidad de plantillas | Las reglas de `angular-eslint` para plantillas |

```
tienda, panel  →  ui, core, api, util
ui             →  util
core           →  api, util
api            →  (nada)
util           →  (nada)
tienda ↔ panel: prohibido
```

Se usa la regla `no-restricted-imports` de ESLint, sin plugins adicionales: es estable entre versiones y
suficiente para este esquema. La plantilla de ejemplo de Angular (`app.html` de `tienda` y `panel`) está
excluida del lint hasta que la historia E0-06 la sustituya.

## Lo que falta

1. **Modo sin Zone.js (zoneless) y `OnPush`**: se confirma con la versión instalada de Angular y se verifica
   que las librerías previstas lo soportan (decisión registrada en la Fase 4, riesgo 1).
2. **`exactOptionalPropertyTypes`**: se activa cuando el esqueleto compile limpio.
3. ~~**Rutas de las librerías**~~ Hecho en E0-06: `tools/link-libraries.mjs` apunta `ui`, `core`, `api` y `util`
   a su código fuente (`projects/<librería>/src/public-api.ts`), de modo que las aplicaciones no necesitan que la
   librería esté compilada antes.
4. **Cliente OpenAPI** en `api`: se genera con el primer endpoint real (historia E1-01).
5. ~~**Tokens y capas de CSS** en `ui`~~ Hecho en E0-06 (ver `docs/design-system.md`).
