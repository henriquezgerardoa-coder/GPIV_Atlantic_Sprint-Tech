#!/usr/bin/env bash
set -euo pipefail

RAIZ_PROYECTO="/home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech"
SCRIPT_SQL="${RAIZ_PROYECTO}/setup/sql/003_renombrar_roles_directivo.sql"
DB_USER="${DB_USER:-admin}"
DB_NAME="${DB_NAME:-gpiv}"

if [ ! -f "${SCRIPT_SQL}" ]; then
  echo "No se encontro el script SQL: ${SCRIPT_SQL}" >&2
  exit 1
fi

if [ -n "${DB_CONTAINER:-}" ]; then
  echo "Aplicando migracion de roles directivo en contenedor '${DB_CONTAINER}' (db=${DB_NAME}, user=${DB_USER})..."
  docker exec -i "${DB_CONTAINER}" psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}" < "${SCRIPT_SQL}"
  echo "Migracion aplicada."
  exit 0
fi

echo "Aplicando migracion de roles directivo con cliente local psql (db=${DB_NAME}, user=${DB_USER})..."
psql \
  -v ON_ERROR_STOP=1 \
  -h "${DB_HOST:-127.0.0.1}" \
  -p "${DB_PORT:-5432}" \
  -U "${DB_USER}" \
  -d "${DB_NAME}" \
  -f "${SCRIPT_SQL}"

echo "Migracion aplicada."

