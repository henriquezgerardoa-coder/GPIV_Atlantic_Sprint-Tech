#!/usr/bin/env bash
set -euo pipefail

PUERTO="${SERVER_PORT:-8090}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ_PROYECTO="$(cd "${SCRIPT_DIR}/.." && pwd)"
ARCHIVO_BLOQUEO="/tmp/gpiv-backend-${PUERTO}.lock"

exec 9>"${ARCHIVO_BLOQUEO}"
if ! flock -n 9; then
  echo "Ya hay un arranque en curso para el puerto ${PUERTO}."
  exit 1
fi

obtener_pids_app() {
  pgrep -f 'com.gpiv.atlanticsprinttech.AplicacionGestionGpiv' | sort -u || true
}

PIDS="$(obtener_pids_app)"
if [ -n "${PIDS}" ]; then
  echo "Deteniendo instancia(s) previa(s) de la aplicacion: ${PIDS}"
  kill ${PIDS} || true
  sleep 2

  PIDS_RESTANTES="$(obtener_pids_app)"
  if [ -n "${PIDS_RESTANTES}" ]; then
    echo "Forzando cierre de PID(s): ${PIDS_RESTANTES}"
    kill -9 ${PIDS_RESTANTES} || true
    sleep 1
  fi
fi

echo "Iniciando backend en puerto ${PUERTO}..."
cd "${RAIZ_PROYECTO}"

# Cargar variables de entorno desde .env si existe (permite usar Neon en local)
if [ -f "${RAIZ_PROYECTO}/.env" ]; then
  set -a
  # shellcheck source=/dev/null
  source "${RAIZ_PROYECTO}/.env"
  set +a
  # Mapear NEON_DB_URL → DB_URL que espera application-dev.properties
  export DB_URL="${NEON_DB_URL:-${DB_URL:-}}"
  export DB_USER="${NEON_DB_USER:-${DB_USER:-admin}}"
  export DB_PASSWORD="${NEON_DB_PASSWORD:-${DB_PASSWORD:-}}"
  echo "Usando base de datos: $(echo "${DB_URL}" | sed 's|//.*@|//***@|')"
fi

echo "Sincronizando modulos (entities/commons/backend)..."
mvn -q -DskipTests install
exec mvn -pl backend spring-boot:run -Dspring-boot.run.arguments=--server.port="${PUERTO}"

