// Añade las tipografías de marca (autoalojadas) a las dos aplicaciones de Angular.
//
// Bricolage Grotesque (títulos) y Figtree (texto) tienen licencia abierta (OFL). Se instalan desde npm con los
// paquetes @fontsource-variable, de modo que se sirven desde la propia tienda y nunca desde Google: así no se
// envía la dirección IP de los visitantes a terceros (RGPD) y se carga más rápido.
//
// Pasos (desde la raíz del proyecto):
//   cd frontend
//   npm install @fontsource-variable/figtree @fontsource-variable/bricolage-grotesque
//   cd ..
//   node tools/install-fonts.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const frontend = resolve(process.cwd(), 'frontend');
const angularPath = resolve(frontend, 'angular.json');
const fonts = [
  'node_modules/@fontsource-variable/figtree/index.css',
  'node_modules/@fontsource-variable/bricolage-grotesque/index.css',
];
if (!existsSync(angularPath)) {
  console.error("No encuentro frontend/angular.json. Ejecuta este script desde la raíz del proyecto.");
  process.exit(1);
}
const missing = fonts.filter((file) => !existsSync(resolve(frontend, file)));
if (missing.length > 0) {
  console.error('Faltan los paquetes de tipografía. Ejecuta primero, dentro de la carpeta frontend:');
  console.error('  npm install @fontsource-variable/figtree @fontsource-variable/bricolage-grotesque');
  process.exit(1);
}

const angular = JSON.parse(readFileSync(angularPath, 'utf8'));
for (const app of ['tienda', 'panel']) {
  const options = angular.projects?.[app]?.architect?.build?.options;
  if (!options) { console.error(`No encuentro la configuración de compilación de ${app}.`); process.exit(1); }
  const styles = options.styles ?? [];
  const missingHere = fonts.filter((font) => !styles.includes(font));
  if (missingHere.length === 0) { console.log(`${app}: las tipografías ya estaban añadidas`); continue; }
  options.styles = [...missingHere, ...styles];
  console.log(`${app}: añadidas ${missingHere.length} tipografías`);
}
writeFileSync(angularPath, JSON.stringify(angular, null, 2) + '\n', 'utf8');
console.log('\nListo. Comprueba con:  cd frontend; npx.cmd ng build tienda');
