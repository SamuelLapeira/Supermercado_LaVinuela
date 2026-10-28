# Frontend: workspace de Angular (historia E0-01)

El workspace **no se escribe a mano**: se crea con el generador oficial de Angular 22, para que
`angular.json` y el resto de ficheros coincidan exactamente con esa versión.

Angular 22 exige **Node.js 22.22.3 o superior, 24.15 o superior, o 26**.

## Estado

Creado y compilando (28-oct-2026): aplicaciones `tienda` y `panel`, librerías `ui`, `core`, `api` y `util`,
TypeScript y plantillas en modo estricto, y límites de tamaño de producción. El paquete inicial de cada
aplicación vacía ocupa unos 217 kB sin comprimir (unos 60 kB comprimidos).

## Volver a crearlo (si hiciera falta)

```powershell
.\tools\bootstrap-frontend.ps1        # Windows
./tools/bootstrap-frontend.sh          # Linux o Mac
cd frontend
npx.cmd ng build tienda
npx.cmd ng build panel
```

## Lo que falta

1. **Reglas de fronteras entre librerías** (ESLint). Se instalan con `ng add angular-eslint` y se añade
   después `eslint-plugin-boundaries`. Reglas a codificar:

   ```
   tienda, panel  →  ui, core, api, util
   ui             →  util            (nunca api, core ni nada de negocio)
   core           →  api, util
   api            →  (solo tipos propios)
   util           →  nada
   tienda ↔ panel: prohibido
   ```

2. **Modo sin Zone.js (zoneless) y `OnPush`**: se confirma con la versión instalada de Angular y se verifica
   que las librerías previstas lo soportan (decisión registrada en la Fase 4, riesgo 1).
3. **`exactOptionalPropertyTypes`**: se activa cuando el esqueleto compile limpio.
4. **Cliente OpenAPI** en `api`: se genera con el primer endpoint real (historia E1-01).
5. **Tokens y capas de CSS** en `ui`: historia E0-06.
