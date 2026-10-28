#!/bin/bash
# Se ejecuta una sola vez, al crear el volumen de MySQL. Crea los usuarios de la Fase 5 (apartado 7.1):
#   app_migrate: DDL completo; solo lo usa Flyway.
#   app_rw:      lectura y escritura de datos; es el que usa la aplicación.
#   app_ro:      solo lectura (informes y consultas de soporte).
set -euo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'app_migrate'@'%' IDENTIFIED BY '${DB_MIGRATE_PASSWORD}';
CREATE USER IF NOT EXISTS 'app_rw'@'%'      IDENTIFIED BY '${DB_APP_PASSWORD}';
CREATE USER IF NOT EXISTS 'app_ro'@'%'      IDENTIFIED BY '${DB_RO_PASSWORD}';

GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO 'app_migrate'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${DB_NAME}\`.* TO 'app_rw'@'%';
GRANT SELECT ON \`${DB_NAME}\`.* TO 'app_ro'@'%';
FLUSH PRIVILEGES;
SQL
