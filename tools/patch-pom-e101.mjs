// Añade al backend/pom.xml lo que necesita el registro con código de verificación (historia E1-01):
//   - spring-boot-starter-mail: envío de correos por SMTP (Mailpit en local);
//   - Bouncy Castle (bcprov-jdk18on): implementación de Argon2id para las contraseñas.
// Se aplica con un script y no con un pom nuevo para no pisar las actualizaciones que haya aceptado Dependabot.
// Es idempotente: se puede ejecutar varias veces.
//
// Uso (desde la raíz del proyecto):   node tools/patch-pom-e101.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

// Versión 1.85 o posterior: las anteriores tienen una vulnerabilidad crítica publicada (CVE-2026-59650).
const BOUNCY_CASTLE_VERSION = '1.85';

const pomPath = resolve(process.cwd(), 'backend', 'pom.xml');
if (!existsSync(pomPath)) {
  console.error('No encuentro backend/pom.xml. Ejecuta este script desde la raíz del proyecto.');
  process.exit(1);
}

let pom = readFileSync(pomPath, 'utf8');
const eol = pom.includes('\r\n') ? '\r\n' : '\n';
pom = pom.replace(/\r\n/g, '\n');
let changed = false;

// 1) Propiedad con la versión de Bouncy Castle.
if (!pom.includes('<bouncycastle.version>')) {
  if (!pom.includes('</properties>')) {
    console.error('No encuentro la sección <properties> del pom. Avísame.');
    process.exit(1);
  }
  pom = pom.replace('</properties>',
    `\n    <!-- Bouncy Castle: implementación de Argon2id. Siempre 1.85 o posterior (CVE-2026-59650). -->\n    <bouncycastle.version>${BOUNCY_CASTLE_VERSION}</bouncycastle.version>\n  </properties>`);
  changed = true;
  console.log(`pom.xml: añadida la versión de Bouncy Castle (${BOUNCY_CASTLE_VERSION})`);
}

// 2) Dependencias.
if (!pom.includes('spring-boot-starter-mail')) {
  const anchors = [
    '    <!-- Observabilidad: comprobaciones de salud y métricas (docs/observability.md). -->',
    '    <!-- Verificación de módulos y eventos entre módulos. -->',
  ];
  const anchor = anchors.find((candidate) => pom.includes(candidate));
  if (!anchor) {
    console.error('No encuentro el punto de inserción de las dependencias en el pom. Avísame.');
    process.exit(1);
  }
  const block = `    <!-- Registro y cuentas (historia E1-01): correo por SMTP y Argon2id para las contraseñas. -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcprov-jdk18on</artifactId>
      <version>\${bouncycastle.version}</version>
    </dependency>

`;
  pom = pom.replace(anchor, block + anchor);
  changed = true;
  console.log('pom.xml: añadidas las dependencias de correo y Bouncy Castle');
}

if (changed) {
  writeFileSync(pomPath, pom.replace(/\n/g, eol), 'utf8');
} else {
  console.log('pom.xml ya estaba actualizado: no se ha cambiado nada');
}
