# Supermercado La Viñuela: plataforma de gestión de pedidos a domicilio

Tienda online con reparto a domicilio para La Viñuela y su comarca, con un panel de gestión pensado para
que la empresa lo maneje sin programadores. Autor del proyecto: Samuel Lapeira Pérez.

El diseño completo está en las fases 1 a 6 de la documentación (análisis, arquitectura, backend, frontend,
base de datos y planificación). Este repositorio contiene la implementación (fase 7).

## Estructura

```
backend/    API: monolito modular con Java 25 y Spring Boot 4.1 (Maven)
frontend/   Angular 22: aplicaciones tienda y panel + librerías (se genera con tools/bootstrap-frontend.sh)
docs/       Decisiones de arquitectura (adr/) y guías
tools/      Scripts de puesta en marcha
```

## Requisitos

| Herramienta | Versión |
| --- | --- |
| JDK | **25** (LTS) |
| Maven | 3.9.6 o superior |
| Node.js | LTS activa (para el frontend) |
| Docker | Para el entorno local (historia E0-02) |

## Backend: comandos

> **Windows (PowerShell):** sigue [docs/windows.md](docs/windows.md); los scripts `.sh` de abajo son para Linux y Mac.

```bash
./tools/dev-up.sh                                        # entorno local (MySQL, Valkey, Mailpit); ver docs/local-environment.md
set -a; source .env; set +a
cd backend
mvn verify                                               # compila (avisos = error), pruebas, módulos y arquitectura
mvn spring-boot:run -Dspring-boot.run.profiles=local     # arranca la aplicación en http://localhost:8080
```

`mvn verify` ejecuta también las pruebas contra un MySQL real (Testcontainers), que necesitan Docker; sin
Docker se saltan.

Para generar el *wrapper* de Maven (una vez): `mvn -N wrapper:wrapper -Dmaven=3.9.11`. A partir de ahí,
`./mvnw verify`.

Los diagramas de módulos se generan en `backend/target/spring-modulith-docs/` al ejecutar las pruebas.

## Reglas que ya hace cumplir el build

- **Avisos del compilador = error** (`-Xlint:all -Werror`): una deprecación rompe la compilación.
- **Fronteras entre módulos** (Spring Modulith): un módulo solo usa a los que declara en su `package-info.java`.
- **Reglas de capas** (ArchUnit): el dominio no conoce Spring ni JPA; la aplicación no usa infraestructura;
  los controladores no tocan persistencia; no hay ciclos entre módulos; el código no lee el reloj del
  sistema directamente (se inyecta un `Clock`); sin `System.out`, `java.util.logging` ni inyección por campo.
- **Exige JDK 25 y Maven 3.9.6+** (maven-enforcer), con mensaje claro si no se cumple.

## Módulos y dependencias permitidas

| Módulo | Puede usar a |
| --- | --- |
| `identity` | `delivery` |
| `catalog` | `content` |
| `inventory` | `catalog` |
| `cart` | `catalog`, `inventory`, `promotions` |
| `delivery` | `content` |
| `ordering` | `identity`, `cart`, `catalog`, `inventory`, `delivery`, `promotions`, `content` |
| `payments` | `ordering` |
| `promotions` | `catalog` |
| `billing` | `ordering`, `payments` |
| `content` | (ninguno) |
| `notifications` | `ordering`, `identity`, `payments`, `inventory`, `content` |
| `reporting` | `ordering`, `catalog`, `inventory` |
| `audit` (compartido) | (ninguno) |
| `shared` (compartido) | (ninguno) |

`audit` y `shared` los puede usar cualquier módulo. El motivo de cada flecha, y el ajuste respecto a la
Fase 3, están en [ADR-011](docs/adr/011-dependencias-entre-modulos.md).

## Cómo se trabaja

Una historia cada vez, en el orden de los sprints. Una historia está terminada cuando cumple sus criterios
de aceptación, tiene pruebas, pasa el lint y las reglas de arquitectura, actualiza el contrato OpenAPI si
lo toca, cumple accesibilidad si toca la interfaz y está documentada.

## Estado: sprint 1

| Historia | Contenido | Estado |
| --- | --- | --- |
| E0-01 | Estructura del repositorio y reglas de arquitectura | Hecha |
| E0-02 | Entorno local con Docker ([docs/local-environment.md](docs/local-environment.md)) | Hecha |
| E0-03 | Migraciones y esquema de la base de datos ([docs/database.md](docs/database.md)) | Hecha |
| E0-04 | Núcleo compartido: dinero, identificadores, errores, paginación y reloj | Hecha |
| E0-05 | Registros, métricas y comprobaciones de salud ([docs/observability.md](docs/observability.md)) | Hecha |
| E0-06 | Base visual: tokens, capas CSS y componentes básicos | Siguiente |
| E0-07 | Integración continua ([docs/ci.md](docs/ci.md)) | Hecha (OpenAPI y cliente generado: con el primer endpoint) |

**Verificado:** `mvn verify` en el backend: 73 pruebas, 0 fallos, 0 saltadas (incluidas las de MySQL real).
El frontend compila (`tienda` y `panel`) en modo estricto con Angular 22.
