// Añade al backend/pom.xml la seguridad de la API (historia E1-02): Spring Security y los tokens de acceso (JWT).
// Se aplica con un script y no con un pom nuevo para no pisar las actualizaciones que haya aceptado Dependabot.
// Es idempotente.
//
// Uso (desde la raíz del proyecto):   node tools/patch-pom-security.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const pomPath = resolve(process.cwd(), 'backend', 'pom.xml');
if (!existsSync(pomPath)) {
  console.error('No encuentro backend/pom.xml. Ejecuta este script desde la raíz del proyecto.');
  process.exit(1);
}

let pom = readFileSync(pomPath, 'utf8');
const eol = pom.includes('\r\n') ? '\r\n' : '\n';
pom = pom.replace(/\r\n/g, '\n');

if (pom.includes('spring-boot-starter-security-oauth2-resource-server')) {
  console.log('pom.xml ya estaba actualizado: no se ha cambiado nada');
  process.exit(0);
}

const anchors = [
  '    <!-- Contrato de la API (docs/api-contract.md): genera el documento OpenAPI a partir del código. -->',
  '    <!-- Registro y cuentas (historia E1-01): correo por SMTP y Argon2id para las contraseñas. -->',
  '    <!-- Observabilidad: comprobaciones de salud y métricas (docs/observability.md). -->',
];
const anchor = anchors.find((candidate) => pom.includes(candidate));
if (!anchor) {
  console.error('No encuentro el punto de inserción de las dependencias en el pom. Avísame.');
  process.exit(1);
}
const block = `    <!-- Seguridad (historia E1-02): Spring Security y tokens de acceso JWT (recurso protegido por token Bearer). -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
    </dependency>

`;
pom = pom.replace(anchor, block + anchor);
writeFileSync(pomPath, pom.replace(/\n/g, eol), 'utf8');
console.log('pom.xml: añadidas las dependencias de seguridad');
