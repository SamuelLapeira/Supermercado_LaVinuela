// Aplica al workspace Angular recién generado las reglas estrictas del proyecto (Fase 4).
// Se ejecuta desde la carpeta 'frontend' (lo hace bootstrap-frontend.sh).
//
//  - TypeScript y plantillas en modo estricto, con comprobaciones adicionales.
//  - Presupuestos de tamaño: la compilación de producción falla si se superan.
//
// Los presupuestos de Angular miden bytes SIN comprimir. El objetivo de la Fase 4 (paquete inicial de la
// tienda ≤ 250 KB comprimido) equivale aproximadamente a 750-800 KB sin comprimir, de ahí los valores.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const workspace = process.cwd();

/** Quita los comentarios de línea y de bloque de un JSON con comentarios (como el tsconfig.json que genera Angular). */
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

function readJson(path) {
  return JSON.parse(stripJsonComments(readFileSync(path, 'utf8')));
}
function writeJson(path, data) {
  writeFileSync(path, JSON.stringify(data, null, 2) + '\n', 'utf8');
}

// 1) tsconfig.json
const tsconfigPath = resolve(workspace, 'tsconfig.json');
if (!existsSync(tsconfigPath)) {
  console.error(`No encuentro ${tsconfigPath}. Ejecuta este script desde la carpeta 'frontend'.`);
  process.exit(1);
}
const tsconfig = readJson(tsconfigPath);
tsconfig.compilerOptions = {
  ...tsconfig.compilerOptions,
  strict: true,
  noImplicitOverride: true,
  noPropertyAccessFromIndexSignature: true,
  noImplicitReturns: true,
  noFallthroughCasesInSwitch: true,
  noUncheckedIndexedAccess: true,
  // exactOptionalPropertyTypes se activará cuando el esqueleto compile limpio (ver docs/frontend-bootstrap.md).
};
tsconfig.angularCompilerOptions = {
  ...tsconfig.angularCompilerOptions,
  strictInjectionParameters: true,
  strictInputAccessModifiers: true,
  strictTemplates: true,
};
writeJson(tsconfigPath, tsconfig);
console.log('tsconfig.json: modo estricto aplicado');

// 2) angular.json: presupuestos de tamaño
const angularPath = resolve(workspace, 'angular.json');
const angular = readJson(angularPath);
const budgets = {
  tienda: { initialWarn: '600kB', initialError: '800kB' },
  panel: { initialWarn: '800kB', initialError: '1MB' },
};
for (const [name, project] of Object.entries(angular.projects ?? {})) {
  const production = project?.architect?.build?.configurations?.production;
  if (!production || !budgets[name]) continue;
  production.budgets = [
    { type: 'initial', maximumWarning: budgets[name].initialWarn, maximumError: budgets[name].initialError },
    { type: 'anyComponentStyle', maximumWarning: '4kB', maximumError: '8kB' },
  ];
  console.log(`angular.json: presupuestos de tamaño aplicados a ${name}`);
}
writeJson(angularPath, angular);
