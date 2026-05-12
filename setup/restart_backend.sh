#!/usr/bin/env bash
set -euo pipefail

PUERTO="${SERVER_PORT:-8090}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ_PROYECTO="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${RAIZ_PROYECTO}/backend/target"
LOG_FILE="${LOG_DIR}/backend-${PUERTO}.log"
SALUD_URL="http://127.0.0.1:${PUERTO}/salud"
HEADERS_URL="http://127.0.0.1:${PUERTO}/ingreso.html"
LOGO_URL="http://127.0.0.1:${PUERTO}/img/logo.png"
PORTADA_URL="http://127.0.0.1:${PUERTO}/img/viedma.jpg"

obtener_pids_app() {
  pgrep -f 'com.gpiv.atlanticsprinttech.AplicacionGestionGpiv|backend-0.0.1-SNAPSHOT.jar' | sort -u || true
}

echo "[1/4] Deteniendo backend previo (si existe)..."
PIDS="$(obtener_pids_app)"
if [ -n "${PIDS}" ]; then
  kill ${PIDS} || true
  sleep 2
  PIDS_RESTANTES="$(obtener_pids_app)"
  if [ -n "${PIDS_RESTANTES}" ]; then
    kill -9 ${PIDS_RESTANTES} || true
    sleep 1
  fi
fi

echo "[2/4] Compilando backend actual..."
cd "${RAIZ_PROYECTO}"
# Build limpio para evitar servir artefactos frontend desactualizados en el backend.
mvn -q -pl frontend,backend -am clean package -DskipTests

echo "[3/4] Iniciando backend en puerto ${PUERTO}..."
mkdir -p "${LOG_DIR}"
nohup java -jar "${RAIZ_PROYECTO}/backend/target/backend-0.0.1-SNAPSHOT.jar" --server.port="${PUERTO}" > "${LOG_FILE}" 2>&1 &
NUEVO_PID=$!

echo "[4/4] Esperando salud y validando cabeceras de iframe..."
for _ in {1..40}; do
  if curl -fsS "${SALUD_URL}" > /dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! curl -fsS "${SALUD_URL}" > /dev/null 2>&1; then
  echo "ERROR: el backend no respondio en ${SALUD_URL}."
  echo "Revisa el log: ${LOG_FILE}"
  exit 1
fi

HEADERS="$(curl -sSI "${HEADERS_URL}")"
XFO="$(printf '%s\n' "${HEADERS}" | awk -F': ' 'tolower($1)=="x-frame-options" {gsub(/\r/,"",$2); print $2}')"
CSP="$(printf '%s\n' "${HEADERS}" | awk -F': ' 'tolower($1)=="content-security-policy" {gsub(/\r/,"",$2); print $2}')"

if [ "${XFO}" != "SAMEORIGIN" ]; then
  echo "ERROR: X-Frame-Options esperado=SAMEORIGIN, actual='${XFO:-<vacio>}'"
  echo "Revisa el log: ${LOG_FILE}"
  exit 1
fi

if [[ "${CSP}" != *"frame-ancestors 'self'"* ]]; then
  echo "ERROR: Content-Security-Policy no contiene frame-ancestors 'self'."
  echo "Actual: ${CSP:-<vacio>}"
  echo "Revisa el log: ${LOG_FILE}"
  exit 1
fi

if ! curl -fsS "${LOGO_URL}" > /dev/null 2>&1; then
  echo "ERROR: no se pudo servir el logo en ${LOGO_URL}."
  echo "Revisa rutas de imagenes y el log: ${LOG_FILE}"
  exit 1
fi

if ! curl -fsS "${PORTADA_URL}" > /dev/null 2>&1; then
  echo "ERROR: no se pudo servir la imagen de portada en ${PORTADA_URL}."
  echo "Revisa rutas de imagenes y el log: ${LOG_FILE}"
  exit 1
fi

echo "Backend reiniciado correctamente. PID=${NUEVO_PID}"
echo "Salud: ${SALUD_URL}"
echo "Headers OK: X-Frame-Options=${XFO} | CSP=${CSP}"
echo "Imagenes OK: ${LOGO_URL} | ${PORTADA_URL}"
echo "Log: ${LOG_FILE}"
