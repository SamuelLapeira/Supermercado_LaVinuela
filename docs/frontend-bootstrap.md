# Frontend: puesta en marcha del workspace (historia E0-01)

El workspace de Angular **no está generado en el repositorio** a propósito: se crea con el generador
oficial de la versión elegida (Angular 22), para que `angular.json` y el resto de ficheros coincidan
exactamente con esa versión en lugar de escribirlos a mano.

## Crearlo

```bash
./tools/bootstrap-frontend.sh
cd frontend
npx ng build tienda
npx ng build panel
```

El script crea las aplicaciones `tienda` y `panel`, las librerías `ui`, `core`, `api` y `util`, y aplica
la configuración estricta (`tools/apply-frontend-config.mjs`).

Si algún parámetro del generador no existe en la versión instalada de Angular, el script se detiene
con un mensaje de error; en ese caso se ajusta la línea indicada (los nombres de las opciones cambian
entre versiones).

## Lo que falta para cerrar la parte de frontend de E0-01

1. **Reglas de fronteras entre librerías** (`eslint-plugin-boundaries` o Sheriff). Se añaden junto con ESLint
   en la historia E0-07 (integración continua), porque ahí se ejecuta el lint. Reglas a codificar:

   ```
   tienda, panel  →  ui, core, api, util
   ui             →  util            (nunca api, core ni nada de negocio)
   core           →  api, util
   api            →  (solo tipos propios)
   util           →  nada
   tienda ↔ panel: prohibido
   ```

2. **Modo sin Zone.js (zoneless) y `OnPush`**: se confirma con la versión instalada de Angular y se
   verifica que las librerías previstas lo soportan. Si alguna no, se vuelve a Zone.js con `OnPush`
   (decisión registrada en la Fase 4, riesgo 1).
3. **`exactOptionalPropertyTypes`**: se activa cuando el esqueleto compile limpio.
4. **Cliente OpenAPI** en `api`: se genera en la historia E0-07.
5. **Tokens y capas de CSS** en `ui`: historia E0-06.
