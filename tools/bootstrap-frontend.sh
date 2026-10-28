#!/usr/bin/env bash
# Crea el workspace Angular de La Viñuela: dos aplicaciones (tienda, panel) y cuatro librerías
# (ui, core, api, util), y aplica la configuración estricta del proyecto.
#
# Se usa el generador oficial de Angular en lugar de escribir angular.json a mano, para que la
# estructura coincida exactamente con la versión elegida.
#
# Requisitos: Node.js LTS activo y npm, con acceso a internet.
# Uso:        ./tools/bootstrap-frontend.sh            (usa Angular 22)
#             ANGULAR_MAJOR=23 ./tools/bootstrap-frontend.sh
set -euo pipefail

ANGULAR_MAJOR="${ANGULAR_MAJOR:-22}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ -e frontend ]; then
  echo "La carpeta 'frontend' ya existe. Bórrala o renómbrala si quieres volver a generarla." >&2
  exit 1
fi
command -v node >/dev/null 2>&1 || { echo "Falta Node.js (versión LTS)." >&2; exit 1; }
command -v npm  >/dev/null 2>&1 || { echo "Falta npm." >&2; exit 1; }

echo ">> Creando el workspace con Angular ${ANGULAR_MAJOR}..."
npx --yes "@angular/cli@${ANGULAR_MAJOR}" new frontend \
  --create-application=false \
  --package-manager=npm \
  --skip-git \
  --interactive=false

cd frontend

echo ">> Aplicaciones..."
npx ng generate application tienda --prefix=lv --style=scss --routing --ssr=false --interactive=false
npx ng generate application panel  --prefix=lv --style=scss --routing --ssr=false --interactive=false

echo ">> Librerías..."
for lib in ui core api util; do
  npx ng generate library "$lib" --prefix=lv --interactive=false
done

echo ">> Configuración estricta del proyecto..."
node ../tools/apply-frontend-config.mjs

echo
echo "Listo. Comprueba que compila:  cd frontend && npx ng build tienda && npx ng build panel"
echo "Siguientes pasos en docs/frontend-bootstrap.md"
