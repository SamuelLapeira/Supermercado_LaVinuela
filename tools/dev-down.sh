#!/usr/bin/env bash
# Para el entorno local. Con --reset borra también los datos (el volumen de MySQL).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [ "${1:-}" = "--reset" ]; then
  echo ">> Parando y BORRANDO los datos locales..."
  docker compose down --volumes
else
  echo ">> Parando contenedores (los datos se conservan)..."
  docker compose down
fi
