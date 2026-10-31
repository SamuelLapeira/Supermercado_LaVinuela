// Añade al backend/pom.xml la librería que genera el contrato OpenAPI (springdoc).
// Solo el documento JSON: no incluye Swagger UI, que no hace falta y ampliaría la superficie de ataque.
// Se aplica con un script y no con un pom nuevo para no pisar las actualizaciones que haya aceptado Dependabot.
// Es idempotente.
//
// Uso (desde la raíz del proyecto):   node tools/patch-pom-openapi.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

// La serie 3.1 de springdoc es la que se construye sobre Spring Boot 4.1.
const SPRINGDOC_VERSION = '3.1.0';

const pomPath = resolve(process.cwd(), 'backend', 'pom.xml');
if (!existsSync(pomPath)) {
  console.error('No encuentro backend/pom.xml. Ejecuta este script desde la raíz del proyecto.');
  process.exit(1);
}

let pom = readFileSync(pomPath, 'utf8');
const eol = pom.includes('\r\n') ? '\r\n' : '\n';
pom = pom.replace(/\r\n/g, '\n');
let changed = false;

if (!pom.includes('<springdoc.version>')) {
  if (!pom.includes('</properties>')) {
    console.error('No encuentro la sección <properties> del pom. Avísame.');
    process.exit(1);
  }
  pom = pom.replace('</properties>',
    `\n    <!-- springdoc: genera el contrato OpenAPI. La serie 3.1 es la de Spring Boot 4.1. -->\n    <springdoc.version>${SPRINGDOC_VERSION}</springdoc.version>\n  </properties>`);
  changed = true;
  console.log(`pom.xml: añadida la versión de springdoc (${SPRINGDOC_VERSION})`);
}

if (!pom.includes('springdoc-openapi-starter-webmvc-api')) {
  const anchors = [
    '    <!-- Registro y cuentas (historia E1-01): correo por SMTP y Argon2id para las contraseñas. -->',
    '    <!-- Observabilidad: comprobaciones de salud y métricas (docs/observability.md). -->',
    '    <!-- Verificación de módulos y eventos entre módulos. -->',
  ];
  const anchor = anchors.find((candidate) => pom.includes(candidate));
  if (!anchor) {
    console.error('No encuentro el punto de inserción de las dependencias en el pom. Avísame.');
    process.exit(1);
  }
  const block = `    <!-- Contrato de la API (docs/api-contract.md): genera el documento OpenAPI a partir del código. -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
      <version>\${springdoc.version}</version>
    </dependency>

`;
  pom = pom.replace(anchor, block + anchor);
  changed = true;
  console.log('pom.xml: añadida la dependencia de springdoc');
}

if (changed) {
  writeFileSync(pomPath, pom.replace(/\n/g, eol), 'utf8');
} else {
  console.log('pom.xml ya estaba actualizado: no se ha cambiado nada');
}
