#!/usr/bin/env bash
set -euo pipefail

RAIZ_PROYECTO="/home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech"
SCRIPT_SQL="${RAIZ_PROYECTO}/setup/sql/007_desasignar_todos_lotes.sql"
DB_USER="${DB_USER:-admin}"
DB_NAME="${DB_NAME:-gpiv}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_PASSWORD="${DB_PASSWORD:-password123}"
DB_URL="${DB_URL:-}"
DB_SSLMODE="${DB_SSLMODE:-}"

if [ -n "${DB_URL}" ]; then
  URL_SIN_PREFIJO="${DB_URL#jdbc:postgresql://}"
  URL_BASE="${URL_SIN_PREFIJO%%\?*}"
  DB_HOSTPORT="${URL_BASE%%/*}"
  DB_NAME="${URL_BASE#*/}"
  DB_HOST="${DB_HOSTPORT%%:*}"
  DB_PORT_CANDIDATO="${DB_HOSTPORT#*:}"
  if [ "${DB_PORT_CANDIDATO}" != "${DB_HOSTPORT}" ]; then
    DB_PORT="${DB_PORT_CANDIDATO}"
  fi
fi

if [ ! -f "${SCRIPT_SQL}" ]; then
  echo "No se encontro el script SQL: ${SCRIPT_SQL}" >&2
  exit 1
fi

if [ -n "${DB_CONTAINER:-}" ]; then
  echo "Desasignando lotes en contenedor '${DB_CONTAINER}' (db=${DB_NAME}, user=${DB_USER})..."
  docker exec -i "${DB_CONTAINER}" psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}" < "${SCRIPT_SQL}"
  echo "Desasignacion aplicada."
  exit 0
fi

if command -v psql >/dev/null 2>&1 && psql --version >/dev/null 2>&1; then
  echo "Desasignando lotes con cliente local psql (db=${DB_NAME}, user=${DB_USER})..."
  if [ -n "${DB_SSLMODE}" ]; then
    export PGSSLMODE="${DB_SSLMODE}"
  fi
  PGPASSWORD="${DB_PASSWORD}" psql \
    -v ON_ERROR_STOP=1 \
    -h "${DB_HOST}" \
    -p "${DB_PORT}" \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    -f "${SCRIPT_SQL}"
elif command -v docker >/dev/null 2>&1; then
  echo "psql local no disponible; usando cliente psql en Docker (db=${DB_NAME}, user=${DB_USER}, host=${DB_HOST}:${DB_PORT})..."
  if [ -n "${DB_SSLMODE}" ]; then
    docker run --rm -i --network host -e PGPASSWORD="${DB_PASSWORD}" -e PGSSLMODE="${DB_SSLMODE}" postgres:16-alpine \
      psql -v ON_ERROR_STOP=1 -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" < "${SCRIPT_SQL}"
  else
    docker run --rm -i --network host -e PGPASSWORD="${DB_PASSWORD}" postgres:16-alpine \
      psql -v ON_ERROR_STOP=1 -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" < "${SCRIPT_SQL}"
  fi
else
  echo "No hay cliente psql ni docker disponible para aplicar la desasignacion." >&2
  exit 1
fi

echo "Desasignacion aplicada."

