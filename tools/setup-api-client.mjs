// Prepara el workspace de Angular para usar el cliente de la API generado a partir del contrato OpenAPI:
//   1. añade el comando "npm run api:generate";
//   2. excluye el código generado del análisis de ESLint (no se edita a mano);
//   3. hace que la librería "api" publique el cliente generado;
//   4. configura el servidor de desarrollo para reenviar /api al servidor de Spring (así no hace falta CORS);
//   5. activa HttpClient en la tienda y en el panel.
// Es idempotente. Uso (desde la raíz del proyecto):   node tools/setup-api-client.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const frontend = resolve(process.cwd(), 'frontend');
if (!existsSync(resolve(frontend, 'angular.json'))) {
  console.error("No encuentro frontend/angular.json. Ejecuta este script desde la raíz del proyecto.");
  process.exit(1);
}
const read = (relative) => readFileSync(resolve(frontend, relative), 'utf8');
const write = (relative, text) => writeFileSync(resolve(frontend, relative), text, 'utf8');

// 1) package.json
const pkg = JSON.parse(read('package.json'));
pkg.scripts ??= {};
if (pkg.scripts['api:generate'] !== 'ng-openapi-gen --config ng-openapi-gen.json') {
  pkg.scripts['api:generate'] = 'ng-openapi-gen --config ng-openapi-gen.json';
  write('package.json', JSON.stringify(pkg, null, 2) + '\n');
  console.log('package.json: añadido "npm run api:generate"');
}
if (!existsSync(resolve(frontend, 'node_modules', 'ng-openapi-gen'))) {
  console.warn('AVISO: falta el generador. Ejecuta dentro de frontend:  npm install --save-dev ng-openapi-gen');
}

// 2) ESLint ignora el código generado
const eslintFile = 'eslint.config.mjs';
if (existsSync(resolve(frontend, eslintFile))) {
  const eslint = read(eslintFile);
  if (!eslint.includes('projects/api/src/lib/generated')) {
    if (eslint.includes("'dist/**',")) {
      write(eslintFile, eslint.replace("'dist/**',", "'projects/api/src/lib/generated/**', 'dist/**',"));
      console.log('eslint.config.mjs: el código generado queda fuera del análisis');
    } else {
      console.warn('AVISO: no he podido excluir el código generado de ESLint; añade projects/api/src/lib/generated/** a "ignores".');
    }
  }
}

// 3) la librería api publica el cliente generado
const publicApi = 'projects/api/src/public-api.ts';
if (!read(publicApi).includes('./lib/generated')) {
  write(publicApi, `/*
 * API pública de la librería api: el cliente de la API del servidor.
 * Todo lo de ./lib/generated se genera con "npm run api:generate" a partir de contracts/openapi.json: no se edita a mano.
 */
export * from './lib/generated';
`);
  console.log('projects/api/src/public-api.ts: publica el cliente generado');
}

// 4) servidor de desarrollo con reenvío de /api
const angular = JSON.parse(read('angular.json'));
let angularChanged = false;
for (const app of ['tienda', 'panel']) {
  const serve = angular.projects?.[app]?.architect?.serve;
  if (!serve) { console.warn(`AVISO: ${app} no tiene destino "serve"`); continue; }
  serve.options ??= {};
  if (serve.options.proxyConfig !== 'proxy.conf.json') {
    serve.options.proxyConfig = 'proxy.conf.json';
    angularChanged = true;
    console.log(`angular.json: ${app} reenvía /api al servidor (localhost:8080)`);
  }
}
if (angularChanged) write('angular.json', JSON.stringify(angular, null, 2) + '\n');

// 5) HttpClient y dirección base de la API en las dos aplicaciones.
//    El contrato declara el servidor como "/" y el generador lo toma como dirección base por defecto; como cada ruta ya
//    empieza por "/", las llamadas saldrían como "//api/..." (el navegador lo entiende como otra web). Por eso se fija
//    la dirección base vacía: las llamadas van a la misma dirección que la página.
for (const app of ['tienda', 'panel']) {
  const file = `projects/${app}/src/app/app.config.ts`;
  if (!existsSync(resolve(frontend, file))) { console.warn(`AVISO: falta ${file}`); continue; }
  let config = read(file);
  const before = config;
  if (!/providers:\s*\[/.test(config)) { console.warn(`AVISO: no he podido modificar ${file}`); continue; }
  if (!config.includes('provideHttpClient')) {
    config = "import { provideHttpClient, withFetch } from '@angular/common/http';\n" + config;
    config = config.replace(/providers:\s*\[/, (match) => `${match}\n    provideHttpClient(withFetch()),`);
  }
  if (!config.includes('provideApiConfiguration')) {
    config = "import { provideApiConfiguration } from 'api';\n" + config;
    config = config.replace(/providers:\s*\[/, (match) => `${match}\n    provideApiConfiguration(''),`);
  }
  if (config !== before) {
    write(file, config);
    console.log(`${file}: HttpClient y dirección base de la API configurados`);
  }
}

console.log('\nListo. Siguiente paso:  cd frontend; npm run api:generate');
