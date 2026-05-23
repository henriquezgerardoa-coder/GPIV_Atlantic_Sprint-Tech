# 🚀 Guía Rápida: Sistema de Gestión de Empleados

## ⚡ Primeros Pasos

### 1️⃣ Crear la tabla en la base de datos
```bash
# Copiar el contenido de este archivo a tu cliente MySQL:
# setup/sql/023_crear_tabla_empleados.sql

mysql -u tu_usuario -p tu_base_de_datos < setup/sql/023_crear_tabla_empleados.sql
```

### 2️⃣ Compilar el proyectol
```bash
mvn clean install -DskipTests
```

### 3️⃣ Iniciar la aplicación
```bash
mvn spring-boot:run
```

## 📡 Probar los Endpoints

### Crear un empleado (como EMPRESA)
```bash
curl -X POST http://localhost:8080/api/empresas/1/empleados \
  -H "Authorization: Bearer TU_TOKEN_EMPRESA" \
  -H "Content-Type: application/json" \
  -d '{
    "cuit": "20123456789",
    "nombre": "Juan Pérez García"
  }'
```

### Ver listado completo (como EMPRESA)
```bash
curl -X GET http://localhost:8080/api/empresas/1/empleados \
  -H "Authorization: Bearer TU_TOKEN_EMPRESA"
```

### Ver cantidad de empleados (como ADMIN)
```bash
curl -X GET http://localhost:8080/api/empresas/1/empleados/cantidad \
  -H "Authorization: Bearer TU_TOKEN_ADMIN"
```

## 📁 Archivos Clave

```
entities/
  └── dominio/
      └── Empleado.java                    ← Entidad

backend/
  ├── repositorio/
  │   └── RepositorioEmpleado.java       ← Datos
  ├── servicio/
  │   ├── ServicioEmpleado.java           ← Interfaz
  │   └── implementacion/
  │       └── ServicioEmpleadoImpl.java   ← Lógica
  ├── controlador/
  │   └── ControladorEmpleado.java       ← API REST
  └── test/
      └── ServicioEmpleadoImplTest.java  ← Pruebas

commons/
  └── comunicacion/dto/
      ├── SolicitudEmpleado.java          ← Input DTO
      ├── RespuestaEmpleado.java          ← Output DTO
      └── RespuestaEmpleadosCantidad.java ← Output cantidad

setup/
  ├── sql/
  │   └── 023_crear_tabla_empleados.sql  ← BD Migration
  └── ejemplos_empleados_curl.sh          ← Ejemplos Curl
```

## 🔐 Permisos por Rol

| Rol | Crear | Ver Listado | Ver Cantidad | Actualizar | Eliminar |
|-----|-------|-------------|--------------|------------|----------|
| EMPRESA | ✅ | ✅ | ❌ | ✅ | ✅ |
| ADMIN | ❌ | ❌ | ✅ | ❌ | ❌ |
| DIRECTIVO | ❌ | ❌ | ✅ | ❌ | ❌ |

## 📊 Base de Datos

Tabla creada: `empleados`

```sql
CREATE TABLE empleados (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cuit VARCHAR(20) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    empresa_id BIGINT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    UNIQUE KEY (empresa_id, cuit),
    FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);
```

## ✅ Validaciones

- ✓ CUIT: Obligatorio, exactamente 11 dígitos
- ✓ Nombre: Obligatorio, 1-120 caracteres
- ✓ Cada empresa: CUIT único
- ✓ Acceso: Validado por rol

## 🧪 Ejecutar Pruebas

```bash
# Todas las pruebas
mvn test

# Solo pruebas de empleados
mvn test -Dtest=ServicioEmpleadoImplTest
```

## 📚 Documentación Completa

- `EMPLEADOS_FEATURE.md` - Descripción detallada
- `IMPLEMENTACION_EMPLEADOS_RESUMEN.md` - Resumen técnico
- `setup/ejemplos_empleados_curl.sh` - Ejemplos de uso

## 🐛 Troubleshooting

**Error: "Tabla empleados no existe"**  
→ Ejecutar el script SQL en `setup/sql/023_crear_tabla_empleados.sql`

**Error: 403 Forbidden**  
→ Verificar que el usuario tiene el rol correcto (EMPRESA, ADMIN o DIRECTIVO)

**Error: 409 Conflict (CUIT duplicado)**  
→ El CUIT ya existe en esa empresa. Usar un CUIT diferente.

**Error: 401 Unauthorized**  
→ Token inválido o expirado. Generar un nuevo token.

## 🎯 Ejemplo Completo de Flujo

```bash
# 1. EMPRESA crea empleado
POST /api/empresas/1/empleados
{
  "cuit": "20123456789",
  "nombre": "Juan Pérez"
}

# 2. EMPRESA ve sus empleados
GET /api/empresas/1/empleados

# 3. ADMIN ve SOLO LA CANTIDAD
GET /api/empresas/1/empleados/cantidad
Response: { "empresaId": 1, "nombreEmpresa": "...", "cantidadEmpleados": 1 }

# 4. EMPRESA actualiza empleado
PUT /api/empresas/1/empleados/1
{
  "cuit": "20123456789",
  "nombre": "Juan Pérez Actualizado"
}

# 5. EMPRESA elimina empleado
DELETE /api/empresas/1/empleados/1
```

---

**Estado**: ✅ Completado y compilado  
**Fecha**: 2024-05-22  
**Java**: 17+  
**Spring Boot**: 3.2.5

