#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8090}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin12345}"
DIRECTIVO_USER="${DIRECTIVO_USER:-directivo}"
DIRECTIVO_PASS="${DIRECTIVO_PASS:-directivo123}"
EMPRESA_USER="${EMPRESA_USER:-empresa}"
EMPRESA_PASS="${EMPRESA_PASS:-empresa12345}"
EMPRESA_ID_USUARIO="${EMPRESA_ID_USUARIO:-}"
AUTO_DETECT_EMPRESA_ID_USUARIO="${AUTO_DETECT_EMPRESA_ID_USUARIO:-true}"

PASS_COUNT=0
FAIL_COUNT=0
LAST_CODE=""

autodetectar_empresa_id_usuario() {
  if [[ -n "$EMPRESA_ID_USUARIO" ]]; then
    return 0
  fi
  if [[ "$AUTO_DETECT_EMPRESA_ID_USUARIO" != "true" ]]; then
    return 0
  fi
  if ! command -v python3 >/dev/null 2>&1; then
    return 0
  fi

  local respuesta
  if ! respuesta="$(curl -sS -u "$ADMIN_USER:$ADMIN_PASS" "$BASE_URL/api/usuarios")"; then
    return 0
  fi

  EMPRESA_ID_USUARIO="$(python3 -c 'import json,sys,os
target=os.environ.get("EMPRESA_USER","empresa").lower()
try:
    data=json.load(sys.stdin)
except Exception:
    print("")
    raise SystemExit(0)
for u in data if isinstance(data, list) else []:
    if str(u.get("nombreUsuario","")).lower() == target:
        print(u.get("id", ""))
        break
else:
    print("")
' <<< "$respuesta")"
}

mostrar_ayuda_credenciales_empresa() {
  echo
  echo "Sugerencia: el usuario '$EMPRESA_USER' devolvio 401 (credenciales invalidas o usuario inactivo)."
  if [[ -n "$EMPRESA_ID_USUARIO" ]]; then
    echo "Reset rapido de clave (ADMIN):"
    echo "curl -u $ADMIN_USER:$ADMIN_PASS -X PATCH -H 'Content-Type: application/json' \\
  -d '{\"claveNueva\":\"$EMPRESA_PASS\"}' \\
  $BASE_URL/api/usuarios/$EMPRESA_ID_USUARIO/clave"
  else
    echo "Define EMPRESA_ID_USUARIO o habilita AUTO_DETECT_EMPRESA_ID_USUARIO=true para mostrar el endpoint exacto de reset."
  fi
}

check_code() {
  local name="$1"
  local expected="$2"
  local url="$3"
  local auth="${4:-}"
  local code

  if [[ -n "$auth" ]]; then
    code="$(curl -s -o /dev/null -w "%{http_code}" -u "$auth" "$url")"
  else
    code="$(curl -s -o /dev/null -w "%{http_code}" "$url")"
  fi
  LAST_CODE="$code"

  if [[ "$code" == "$expected" ]]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    printf 'PASS %-36s expected=%s got=%s\n' "$name" "$expected" "$code"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    printf 'FAIL %-36s expected=%s got=%s\n' "$name" "$expected" "$code"
  fi
}

echo "== QA permisos HTTP =="
echo "Base URL: $BASE_URL"
echo

autodetectar_empresa_id_usuario

check_code "salud" "200" "$BASE_URL/salud"
check_code "admin empresas" "200" "$BASE_URL/api/empresas" "$ADMIN_USER:$ADMIN_PASS"
check_code "admin vista empresas" "200" "$BASE_URL/api/empresas/admin/vista" "$ADMIN_USER:$ADMIN_PASS"
check_code "admin usuarios" "200" "$BASE_URL/api/usuarios" "$ADMIN_USER:$ADMIN_PASS"
check_code "directivo empresas" "200" "$BASE_URL/api/empresas" "$DIRECTIVO_USER:$DIRECTIVO_PASS"
check_code "directivo vista empresas" "403" "$BASE_URL/api/empresas/admin/vista" "$DIRECTIVO_USER:$DIRECTIVO_PASS"
check_code "directivo usuarios" "403" "$BASE_URL/api/usuarios" "$DIRECTIVO_USER:$DIRECTIVO_PASS"
check_code "empresa radicaciones" "200" "$BASE_URL/api/radicaciones" "$EMPRESA_USER:$EMPRESA_PASS"
EMPRESA_RAD_CODE="$LAST_CODE"
check_code "empresa usuarios" "403" "$BASE_URL/api/usuarios" "$EMPRESA_USER:$EMPRESA_PASS"
EMPRESA_USUARIOS_CODE="$LAST_CODE"
check_code "anon empresas" "401" "$BASE_URL/api/empresas"

if [[ "$EMPRESA_RAD_CODE" == "401" || "$EMPRESA_USUARIOS_CODE" == "401" ]]; then
  mostrar_ayuda_credenciales_empresa
fi

echo
printf 'Resumen: PASS=%d FAIL=%d\n' "$PASS_COUNT" "$FAIL_COUNT"

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  exit 1
fi

