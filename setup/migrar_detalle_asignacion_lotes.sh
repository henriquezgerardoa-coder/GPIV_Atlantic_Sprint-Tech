#!/usr/bin/env bash
set -euo pipefail

RAIZ_PROYECTO="/home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech"
SCRIPT_SQL="${RAIZ_PROYECTO}/setup/sql/006_detalle_asignacion_lotes.sql"
DB_USER="${DB_USER:-admin}"
DB_NAME="${DB_NAME:-gpiv}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_PASSWORD="${DB_PASSWORD:-password123}"

if [ ! -f "${SCRIPT_SQL}" ]; then
  echo "No se encontro el script SQL: ${SCRIPT_SQL}" >&2
  exit 1
fi

if [ -n "${DB_CONTAINER:-}" ]; then
  echo "Aplicando migracion de detalle de asignacion en lotes en contenedor '${DB_CONTAINER}' (db=${DB_NAME}, user=${DB_USER})..."
  docker exec -i "${DB_CONTAINER}" psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}" < "${SCRIPT_SQL}"
  echo "Migracion aplicada."
  exit 0
fi

if command -v psql >/dev/null 2>&1; then
  echo "Aplicando migracion de detalle de asignacion de lotes con cliente local psql (db=${DB_NAME}, user=${DB_USER})..."
  PGPASSWORD="${DB_PASSWORD}" psql \
    -v ON_ERROR_STOP=1 \
    -h "${DB_HOST}" \
    -p "${DB_PORT}" \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    -f "${SCRIPT_SQL}"
elif command -v docker >/dev/null 2>&1; then
  echo "psql local no disponible; usando cliente psql en Docker (db=${DB_NAME}, user=${DB_USER}, host=${DB_HOST}:${DB_PORT})..."
  docker run --rm -i --network host -e PGPASSWORD="${DB_PASSWORD}" postgres:16-alpine \
    psql -v ON_ERROR_STOP=1 -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" < "${SCRIPT_SQL}"
else
  echo "No hay cliente psql ni docker disponible para aplicar la migracion." >&2
  exit 1
fi

echo "Migracion aplicada."

