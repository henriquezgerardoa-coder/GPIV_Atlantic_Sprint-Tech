#!/usr/bin/env bash
set -euo pipefail

RAIZ_PROYECTO="/home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech"
DB_USER="${DB_USER:-admin}"
DB_NAME="${DB_NAME:-gpiv}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_PASSWORD="${DB_PASSWORD:-password123}"
DB_URL="${DB_URL:-}"
DB_SSLMODE="${DB_SSLMODE:-}"
API_CHECK="false"
API_BASE_URL="${API_BASE_URL:-http://localhost:8090}"
API_USER="${API_USER:-admin}"
API_PASSWORD="${API_PASSWORD:-admin12345}"

for arg in "$@"; do
  case "$arg" in
    --api-check)
      API_CHECK="true"
      ;;
    *)
      echo "Uso: $0 [--api-check]" >&2
      exit 1
      ;;
  esac
done

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

read -r -d '' SQL_RESUMEN <<'SQL' || true
WITH resumen AS (
  SELECT
    COUNT(*) AS total_lotes,
    COUNT(*) FILTER (WHERE empresa_id IS NULL) AS lotes_sin_empresa,
    COUNT(*) FILTER (WHERE empresa_id IS NOT NULL) AS lotes_con_empresa,
    COUNT(*) FILTER (
      WHERE empresa_id IS NULL
        AND (
          fecha_asignacion IS NOT NULL
          OR estado_asignacion IS NOT NULL
          OR numero_expediente_referencia IS NOT NULL
        )
    ) AS inconsistencias_sin_empresa
  FROM lotes
)
SELECT
  total_lotes,
  lotes_sin_empresa,
  lotes_con_empresa,
  inconsistencias_sin_empresa
FROM resumen;
SQL

run_psql() {
  local sql="$1"

  if [ -n "${DB_CONTAINER:-}" ]; then
    docker exec -i "${DB_CONTAINER}" psql -v ON_ERROR_STOP=1 -At -F '|' -U "${DB_USER}" -d "${DB_NAME}" -c "$sql"
    return
  fi

  if command -v psql >/dev/null 2>&1 && psql --version >/dev/null 2>&1; then
    if [ -n "${DB_SSLMODE}" ]; then
      export PGSSLMODE="${DB_SSLMODE}"
    fi
    PGPASSWORD="${DB_PASSWORD}" psql -v ON_ERROR_STOP=1 -At -F '|' \
      -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "$sql"
    return
  fi

  if command -v docker >/dev/null 2>&1; then
    if [ -n "${DB_SSLMODE}" ]; then
      docker run --rm --network host -e PGPASSWORD="${DB_PASSWORD}" -e PGSSLMODE="${DB_SSLMODE}" postgres:16-alpine \
        psql -v ON_ERROR_STOP=1 -At -F '|' -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "$sql"
    else
      docker run --rm --network host -e PGPASSWORD="${DB_PASSWORD}" postgres:16-alpine \
        psql -v ON_ERROR_STOP=1 -At -F '|' -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "$sql"
    fi
    return
  fi

  echo "ERROR: no hay psql funcional ni docker disponible." >&2
  exit 2
}

if ! run_psql "SELECT to_regclass('public.lotes');" >/tmp/gpiv_lotes_table_check.txt 2>/tmp/gpiv_lotes_table_check.err; then
  echo "ERROR: no se pudo consultar la base de datos." >&2
  cat /tmp/gpiv_lotes_table_check.err >&2
  exit 2
fi

TABLA_LOTES="$(cat /tmp/gpiv_lotes_table_check.txt | tr -d '[:space:]')"
if [ "${TABLA_LOTES}" != "lotes" ] && [ "${TABLA_LOTES}" != "public.lotes" ]; then
  echo "ERROR: la tabla 'lotes' no existe en la base objetivo (${DB_HOST}:${DB_PORT}/${DB_NAME})." >&2
  exit 3
fi

RESUMEN="$(run_psql "${SQL_RESUMEN}")"
IFS='|' read -r TOTAL LOTES_SIN LOTES_CON INCONSISTENCIAS <<<"${RESUMEN}"

echo "Base objetivo: ${DB_HOST}:${DB_PORT}/${DB_NAME} (user=${DB_USER})"
echo "Total lotes: ${TOTAL}"
echo "Lotes sin empresa: ${LOTES_SIN}"
echo "Lotes con empresa: ${LOTES_CON}"
echo "Inconsistencias (sin empresa pero con metadatos): ${INCONSISTENCIAS}"

ESTADO="OK"
if [ "${LOTES_CON}" != "0" ] || [ "${INCONSISTENCIAS}" != "0" ]; then
  ESTADO="FAIL"
fi

if [ "${API_CHECK}" = "true" ]; then
  echo ""
  echo "Validacion API: ${API_BASE_URL}/api/lotes"
  if ! command -v curl >/dev/null 2>&1; then
    echo "WARN: curl no disponible; se omite validacion API." >&2
  elif ! command -v python3 >/dev/null 2>&1; then
    echo "WARN: python3 no disponible; se omite validacion API." >&2
  else
    API_RAW="$(curl -sS -u "${API_USER}:${API_PASSWORD}" "${API_BASE_URL}/api/lotes" || true)"
    if [ -z "${API_RAW}" ]; then
      echo "WARN: respuesta vacia de API." >&2
    else
      python3 - <<'PY' "${API_RAW}"
import json
import sys

raw = sys.argv[1]
try:
    data = json.loads(raw)
except Exception:
    print("WARN: respuesta API no es JSON valido")
    sys.exit(0)

if not isinstance(data, list):
    print("WARN: respuesta API no es una lista")
    sys.exit(0)

total = len(data)
con_empresa = sum(1 for x in data if x.get("empresaId") is not None)
sin_asignar = sum(1 for x in data if x.get("nombreEmpresa") == "Sin asignar")
print(f"API total lotes: {total}")
print(f"API lotes con empresaId: {con_empresa}")
print(f"API lotes con nombreEmpresa='Sin asignar': {sin_asignar}")
PY
    fi
  fi
fi

echo ""
echo "Resultado verificacion: ${ESTADO}"
if [ "${ESTADO}" = "OK" ]; then
  exit 0
fi

exit 4

