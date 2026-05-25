#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ_PROYECTO="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${RAIZ_PROYECTO}/backend/compose-local.yaml"

obtener_pids_app() {
  pgrep -f 'com.gpiv.atlanticsprinttech.AplicacionGestionGpiv' | sort -u || true
}

puerto_ocupado() {
  local puerto="$1"
  ss -ltn "( sport = :${puerto} )" | grep -q ":${puerto}"
}

detener_backend() {
  local pids
  pids="$(obtener_pids_app)"

  if [ -z "${pids}" ]; then
    echo "No hay instancia de backend en ejecucion."
    return 0
  fi

  echo "Deteniendo backend (PID/s: ${pids})..."
  kill ${pids} || true
  sleep 2

  local restantes
  restantes="$(obtener_pids_app)"
  if [ -n "${restantes}" ]; then
    echo "Forzando cierre de backend (PID/s: ${restantes})..."
    kill -9 ${restantes} || true
    sleep 1
  fi

  echo "Backend detenido."
}

detener_compose() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker no esta disponible; omito detener compose."
    return 0
  fi

  if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "No se encontro ${COMPOSE_FILE}; omito detener compose."
    return 0
  fi

  echo "Deteniendo servicios docker compose (${COMPOSE_FILE})..."
  docker compose --file "${COMPOSE_FILE}" --ansi never down --remove-orphans >/dev/null 2>&1 || true

  if puerto_ocupado 5433; then
    echo "Aviso: el puerto 5433 sigue ocupado por un proceso externo al compose de este proyecto."
  else
    echo "Puerto 5433 liberado."
  fi
}

mostrar_estado_puertos() {
  echo "Estado de puertos relevantes:"
  ss -ltnp | grep -E ':(8090|5433)' || echo "Sin procesos escuchando en 8090/5433"
}

detener_backend
detener_compose
mostrar_estado_puertos
