// Hace que las aplicaciones usen las librerías (ui, core, api, util) desde su código fuente, y no desde la carpeta
// dist. "ng generate library" las apunta a dist/<librería>, lo que obliga a compilar las librerías antes que las
// aplicaciones y rompe el desarrollo diario (ng serve, ng test).
//
// Se ejecuta una vez desde la raíz del proyecto:   node tools/link-libraries.mjs
// Es idempotente.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const tsconfigPath = resolve(process.cwd(), 'frontend', 'tsconfig.json');
if (!existsSync(tsconfigPath)) {
  console.error("No encuentro frontend/tsconfig.json. Ejecuta este script desde la raíz del proyecto.");
  process.exit(1);
}

/** Quita los comentarios de línea y de bloque de un JSON con comentarios. */
function stripJsonComments(text) {
  let out = '';
  let inString = false;
  let escaped = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    const n = text[i + 1];
    if (inString) {
      out += c;
      if (escaped) escaped = false;
      else if (c === '\\') escaped = true;
      else if (c === '"') inString = false;
      continue;
    }
    if (c === '"') { inString = true; out += c; continue; }
    if (c === '/' && n === '/') { while (i < text.length && text[i] !== '\n') i++; out += '\n'; continue; }
    if (c === '/' && n === '*') { i += 2; while (i < text.length && !(text[i] === '*' && text[i + 1] === '/')) i++; i++; continue; }
    out += c;
  }
  return out;
}

const tsconfig = JSON.parse(stripJsonComments(readFileSync(tsconfigPath, 'utf8')));
tsconfig.compilerOptions ??= {};
tsconfig.compilerOptions.paths ??= {};
let changed = false;
for (const lib of ['ui', 'core', 'api', 'util']) {
  const target = [`./projects/${lib}/src/public-api.ts`];
  const current = tsconfig.compilerOptions.paths[lib];
  if (JSON.stringify(current) !== JSON.stringify(target)) {
    tsconfig.compilerOptions.paths[lib] = target;
    changed = true;
    console.log(`tsconfig.json: "${lib}" apunta ahora a ${target[0]}`);
  }
}
if (changed) {
  writeFileSync(tsconfigPath, JSON.stringify(tsconfig, null, 2) + '\n', 'utf8');
} else {
  console.log('tsconfig.json ya apuntaba a las fuentes: no se ha cambiado nada');
}
