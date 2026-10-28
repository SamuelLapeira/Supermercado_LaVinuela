#!/usr/bin/env bash
# Levanta el entorno local (MySQL, Valkey y Mailpit) y deja listo el fichero .env.
# La primera vez crea .env con contraseñas aleatorias.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

command -v docker >/dev/null 2>&1 || { echo "Falta Docker." >&2; exit 1; }

if [ ! -f .env ]; then
  echo ">> Creando .env con contraseñas aleatorias..."
  random() { LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24; }
  sed -e "s/^MYSQL_ROOT_PASSWORD=.*/MYSQL_ROOT_PASSWORD=$(random)/" \
      -e "s/^DB_MIGRATE_PASSWORD=.*/DB_MIGRATE_PASSWORD=$(random)/" \
      -e "s/^DB_APP_PASSWORD=.*/DB_APP_PASSWORD=$(random)/" \
      -e "s/^DB_RO_PASSWORD=.*/DB_RO_PASSWORD=$(random)/" \
      .env.example > .env
  chmod 600 .env
fi

echo ">> Arrancando contenedores..."
docker compose up -d --wait

echo
echo "Entorno listo:"
echo "  MySQL    127.0.0.1:$(grep -E '^DB_PORT=' .env | cut -d= -f2)   (base de datos: $(grep -E '^DB_NAME=' .env | cut -d= -f2))"
echo "  Valkey   127.0.0.1:$(grep -E '^REDIS_PORT=' .env | cut -d= -f2)"
echo "  Mailpit  http://127.0.0.1:$(grep -E '^MAIL_UI_PORT=' .env | cut -d= -f2)   (SMTP en el puerto $(grep -E '^MAIL_SMTP_PORT=' .env | cut -d= -f2))"
echo
echo "Para arrancar la API con ese entorno:"
echo "  set -a; source .env; set +a"
echo "  cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local"
