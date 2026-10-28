// Añade al backend/pom.xml lo que necesita la historia E0-05 (salud, métricas e información de compilación).
// Se aplica con un script, y no con un pom nuevo, para no pisar las actualizaciones de versiones que haya
// aceptado Dependabot. Se puede ejecutar varias veces: no duplica nada.
//
// Uso (desde la raíz del proyecto):   node tools/patch-pom-e005.mjs
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
let changed = false;

// 1) Dependencias: actuator (salud y métricas) y el registro de métricas en formato Prometheus.
if (!pom.includes('spring-boot-starter-actuator')) {
  const anchor = '    <!-- Verificación de módulos y eventos entre módulos. -->';
  if (!pom.includes(anchor)) {
    console.error('No encuentro el punto de inserción de las dependencias en el pom. Avísame.');
    process.exit(1);
  }
  const block = `    <!-- Observabilidad: comprobaciones de salud y métricas (docs/observability.md). -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
      <scope>runtime</scope>
    </dependency>

`;
  pom = pom.replace(anchor, block + anchor);
  changed = true;
  console.log('pom.xml: añadidas las dependencias de actuator y Prometheus');
}

// 2) Información de compilación (versión y fecha) para el punto /actuator/info.
if (!pom.includes('<goal>build-info</goal>')) {
  const pluginRegex = /(<artifactId>spring-boot-maven-plugin<\/artifactId>)(\s*)(<\/plugin>)/;
  if (!pluginRegex.test(pom)) {
    console.error('No encuentro el plugin de Spring Boot en el pom. Avísame.');
    process.exit(1);
  }
  pom = pom.replace(pluginRegex,
    `$1
        <executions>
          <execution>
            <id>build-info</id>
            <goals>
              <goal>build-info</goal>
            </goals>
          </execution>
        </executions>
      $3`);
  changed = true;
  console.log('pom.xml: añadida la información de compilación');
}

if (changed) {
  writeFileSync(pomPath, pom.replace(/\n/g, eol), 'utf8');
} else {
  console.log('pom.xml ya estaba actualizado: no se ha cambiado nada');
}
