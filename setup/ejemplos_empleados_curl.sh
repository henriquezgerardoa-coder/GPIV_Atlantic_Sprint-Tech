#!/bin/bash

# Ejemplos de requests curl para la API de Empleados
# Reemplazar:
# - BASE_URL: URL base del servidor (ej: http://localhost:8080)
# - TOKEN_EMPRESA: Token JWT del usuario EMPRESA
# - TOKEN_ADMIN: Token JWT del usuario ADMIN
# - TOKEN_DIRECTIVO: Token JWT del usuario DIRECTIVO

BASE_URL="http://localhost:8080"
EMPRESA_ID=1
TOKEN_EMPRESA="tu_token_empresa_aqui"
TOKEN_ADMIN="tu_token_admin_aqui"
TOKEN_DIRECTIVO="tu_token_directivo_aqui"

# ============================================
# 1. CREAR UN EMPLEADO (Solo EMPRESA)
# ============================================
echo "=== 1. Crear un empleado ==="
curl -X POST "$BASE_URL/api/empresas/$EMPRESA_ID/empleados" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json" \
  -d '{
    "cuit": "20123456789",
    "nombre": "Juan Pérez García"
  }'

echo -e "\n\n"

# ============================================
# 2. LISTAR EMPLEADOS DE LA EMPRESA (Solo EMPRESA)
# ============================================
echo "=== 2. Listar empleados de la empresa ==="
curl -X GET "$BASE_URL/api/empresas/$EMPRESA_ID/empleados" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json"

echo -e "\n\n"

# ============================================
# 3. OBTENER UN EMPLEADO ESPECÍFICO (Solo EMPRESA)
# ============================================
echo "=== 3. Obtener empleado específico ==="
EMPLEADO_ID=1
curl -X GET "$BASE_URL/api/empresas/$EMPRESA_ID/empleados/$EMPLEADO_ID" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json"

echo -e "\n\n"

# ============================================
# 4. OBTENER CANTIDAD DE EMPLEADOS (Solo ADMIN y DIRECTIVO)
# ============================================
echo "=== 4. Obtener cantidad de empleados (Como ADMIN) ==="
curl -X GET "$BASE_URL/api/empresas/$EMPRESA_ID/empleados/cantidad" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json"

echo -e "\n\n"

echo "=== 4b. Obtener cantidad de empleados (Como DIRECTIVO) ==="
curl -X GET "$BASE_URL/api/empresas/$EMPRESA_ID/empleados/cantidad" \
  -H "Authorization: Bearer $TOKEN_DIRECTIVO" \
  -H "Content-Type: application/json"

echo -e "\n\n"

# ============================================
# 5. ACTUALIZAR EMPLEADO (Solo EMPRESA)
# ============================================
echo "=== 5. Actualizar empleado ==="
curl -X PUT "$BASE_URL/api/empresas/$EMPRESA_ID/empleados/$EMPLEADO_ID" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json" \
  -d '{
    "cuit": "20123456789",
    "nombre": "Juan Pérez García - ACTUALIZADO"
  }'

echo -e "\n\n"

# ============================================
# 6. ELIMINAR EMPLEADO (Solo EMPRESA)
# ============================================
echo "=== 6. Eliminar empleado ==="
curl -X DELETE "$BASE_URL/api/empresas/$EMPRESA_ID/empleados/$EMPLEADO_ID" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json"

echo -e "\n\n"

# ============================================
# EJEMPLOS DE ERRORES
# ============================================

echo "=== ERROR: EMPRESA intenta ver cantidad (Forbidden) ==="
curl -X GET "$BASE_URL/api/empresas/$EMPRESA_ID/empleados/cantidad" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json"

echo -e "\n\n"

echo "=== ERROR: ADMIN intenta crear empleado (Forbidden) ==="
curl -X POST "$BASE_URL/api/empresas/$EMPRESA_ID/empleados" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "cuit": "20123456789",
    "nombre": "Juan Pérez"
  }'

echo -e "\n\n"

echo "=== ERROR: CUIT inválido (Bad Request) ==="
curl -X POST "$BASE_URL/api/empresas/$EMPRESA_ID/empleados" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json" \
  -d '{
    "cuit": "123",
    "nombre": "Juan Pérez"
  }'

echo -e "\n\n"

echo "=== ERROR: CUIT duplicado (Conflict) ==="
curl -X POST "$BASE_URL/api/empresas/$EMPRESA_ID/empleados" \
  -H "Authorization: Bearer $TOKEN_EMPRESA" \
  -H "Content-Type: application/json" \
  -d '{
    "cuit": "20123456789",
    "nombre": "Otro empleado"
  }'

echo -e "\n"

