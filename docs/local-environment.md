# Entorno local (historia E0-02)

Un solo comando levanta todo lo que la aplicación necesita para desarrollar.

## Qué incluye

| Servicio | Para qué | Dirección |
| --- | --- | --- |
| MySQL 8.4 | Base de datos | `127.0.0.1:3306` |
| Valkey (compatible con Redis) | Caché y límites de ritmo (se usará en historias posteriores) | `127.0.0.1:6379` |
| Mailpit | Captura los correos que envíe la aplicación; no sale ninguno a internet | Interfaz en `http://127.0.0.1:8025`, SMTP en el puerto 1025 |

Todos escuchan solo en `127.0.0.1`: no se pueden alcanzar desde otras máquinas.

## Arrancar

```bash
./tools/dev-up.sh
```

La primera vez crea el fichero `.env` con contraseñas aleatorias (no se sube a Git). Después:

```bash
set -a; source .env; set +a
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Al arrancar, Flyway aplica las migraciones con el usuario `app_migrate` y la aplicación trabaja con
`app_rw`, que no puede cambiar la estructura de la base de datos.

## Parar

```bash
./tools/dev-down.sh           # para los contenedores y conserva los datos
./tools/dev-down.sh --reset   # para y BORRA los datos locales
```

## Usuarios de base de datos

Se crean la primera vez que arranca MySQL (`docker/mysql/init/01-create-users.sh`):

| Usuario | Permisos | Quién lo usa |
| --- | --- | --- |
| `app_migrate` | Todos, sobre la base `shop` | Solo Flyway |
| `app_rw` | Leer y escribir datos | La aplicación |
| `app_ro` | Solo leer | Informes y consultas de soporte |

## Decisiones de esta historia

- **Valkey en lugar de Redis.** Es compatible con el protocolo de Redis y tiene licencia BSD, sin dudas
  sobre licencias. Para la aplicación es indiferente; la decisión definitiva se confirma con el despliegue.
- **No se incluye MinIO** (almacenamiento compatible con S3). En desarrollo bastará con una carpeta local
  mientras no se haya elegido el alojamiento; el proveedor de almacenamiento se decidirá con él y la
  aplicación lo usará a través de un puerto (`ObjectStorage`).
- **`log_bin_trust_function_creators=1`.** MySQL lo exige para crear *triggers* con el registro binario
  activado. Los servicios gestionados suelen traerlo configurado; hay que comprobarlo al elegir alojamiento.
- Las imágenes de Mailpit usan la etiqueta `latest` para facilitar el desarrollo; se fijará una versión
  concreta cuando se prepare la integración continua.
