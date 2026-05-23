# Funcionalidad: Gestión de Empleados por Empresa

## Descripción General

Se ha implementado un sistema de gestión de empleados que permite a cada empresa cargar y mantener un listado de sus empleados con CUIT y nombre. El acceso a esta información está controlado por roles:

- **EMPRESA**: Pueden cargar, ver el listado completo, actualizar y eliminar sus empleados
- **ADMIN y DIRECTIVO**: Solo pueden ver la cantidad de empleados cargados por empresa

## Estructura Implementada

### 1. Entidad de Base de Datos: `Empleado`
**Ubicación**: `entities/src/main/java/com/gpiv/atlanticsprinttech/entities/dominio/Empleado.java`

**Campos**:
- `id` (Long): Identificador único autogenerado
- `cuit` (String): CUIT del empleado (11 dígitos)
- `nombre` (String): Nombre del empleado
- `empresa_id` (Long): Referencia a la empresa dueña del empleado
- `fechaRegistro` (LocalDateTime): Fecha de creación del registro

**Restricciones**:
- Unique constraint en (empresa_id, cuit) - No puede haber dos empleados con el mismo CUIT en la misma empresa

### 2. Repositorio: `RepositorioEmpleado`
**Ubicación**: `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/repositorio/RepositorioEmpleado.java`

**Métodos principales**:
- `findByEmpresaId()`: Obtiene todos los empleados de una empresa
- `findByIdAndEmpresaId()`: Obtiene un empleado específico verificando pertenencia
- `countByEmpresaId()`: Cuenta empleados de una empresa
- `existsByEmpresaIdAndCuit()`: Verifica duplicidad de CUIT

### 3. DTOs de Comunicación

#### SolicitudEmpleado
**Ubicación**: `commons/src/main/java/com/gpiv/atlanticsprinttech/commons/comunicacion/dto/SolicitudEmpleado.java`

```json
{
  "cuit": "11223344556",
  "nombre": "Juan Pérez"
}
```

#### RespuestaEmpleado
**Ubicación**: `commons/src/main/java/com/gpiv/atlanticsprinttech/commons/comunicacion/dto/RespuestaEmpleado.java`

```json
{
  "id": 1,
  "cuit": "11223344556",
  "nombre": "Juan Pérez",
  "fechaRegistro": "2024-05-22T10:30:00"
}
```

#### RespuestaEmpleadosCantidad
**Ubicación**: `commons/src/main/java/com/gpiv/atlanticsprinttech/commons/comunicacion/dto/RespuestaEmpleadosCantidad.java`

```json
{
  "empresaId": 1,
  "nombreEmpresa": "Mi Empresa S.A.",
  "cantidadEmpleados": 15
}
```

### 4. Servicio: `ServicioEmpleado`

**Ubicación**: 
- Interfaz: `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/servicio/ServicioEmpleado.java`
- Implementación: `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/servicio/implementacion/ServicioEmpleadoImpl.java`

**Métodos principales**:
- `crear()`: Crea un nuevo empleado (solo EMPRESA)
- `obtenerPorId()`: Obtiene un empleado específico
- `listarPorEmpresa()`: Lista todos los empleados de una empresa (solo EMPRESA)
- `obtenerCantidadPorEmpresa()`: Obtiene solo la cantidad (solo ADMIN/DIRECTIVO)
- `actualizar()`: Actualiza datos del empleado (solo EMPRESA)
- `eliminar()`: Elimina un empleado (solo EMPRESA)

### 5. Controlador: `ControladorEmpleado`
**Ubicación**: `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/controlador/ControladorEmpleado.java`

## Endpoints API

### Crear Empleado
```
POST /api/empresas/{empresaId}/empleados
Authorization: Bearer token
Content-Type: application/json

{
  "cuit": "11223344556",
  "nombre": "Juan Pérez"
}
```
**Respuesta**: `201 Created` con el empleado creado
**Permisos**: Solo EMPRESA de la empresa correspondiente

---

### Listar Empleados de una Empresa
```
GET /api/empresas/{empresaId}/empleados
Authorization: Bearer token
```
**Respuesta**: Lista completa de empleados con todos los detalles
**Permisos**: Solo EMPRESA de la empresa correspondiente

---

### Obtener Empleado por ID
```
GET /api/empresas/{empresaId}/empleados/{empleadoId}
Authorization: Bearer token
```
**Respuesta**: Datos completos del empleado
**Permisos**: Solo EMPRESA de la empresa correspondiente

---

### Obtener Cantidad de Empleados
```
GET /api/empresas/{empresaId}/empleados/cantidad
Authorization: Bearer token
```
**Respuesta**:
```json
{
  "empresaId": 1,
  "nombreEmpresa": "Mi Empresa S.A.",
  "cantidadEmpleados": 15
}
```
**Permisos**: Solo ADMIN y DIRECTIVO

---

### Actualizar Empleado
```
PUT /api/empresas/{empresaId}/empleados/{empleadoId}
Authorization: Bearer token
Content-Type: application/json

{
  "cuit": "11223344556",
  "nombre": "Juan Pérez actualizado"
}
```
**Respuesta**: `200 OK` con el empleado actualizado
**Permisos**: Solo EMPRESA de la empresa correspondiente

---

### Eliminar Empleado
```
DELETE /api/empresas/{empresaId}/empleados/{empleadoId}
Authorization: Bearer token
```
**Respuesta**: `204 No Content`
**Permisos**: Solo EMPRESA de la empresa correspondiente

## Control de Acceso y Permisos

### Por Rol de Usuario:

| Acción | EMPRESA | ADMIN | DIRECTIVO | AUDITOR | TECNICO |
|--------|---------|-------|-----------|---------|---------|
| Crear Empleado | ✓ | ✗ | ✗ | ✗ | ✗ |
| Ver Listado Completo | ✓ | ✗ | ✗ | ✗ | ✗ |
| Ver Detalles de Empleado | ✓ | ✗ | ✗ | ✗ | ✗ |
| Ver Cantidad | ✗ | ✓ | ✓ | ✗ | ✗ |
| Actualizar | ✓ | ✗ | ✗ | ✗ | ✗ |
| Eliminar | ✓ | ✗ | ✗ | ✗ | ✗ |

### Validaciones Implementadas:

1. **Validación de Empresa**: Los usuarios EMPRESA solo pueden operar con su propia empresa
2. **Duplicidad de CUIT**: No se permite registrar el mismo CUIT dos veces en la misma empresa
3. **Roles Requeridos**: Se verifica el rol del usuario antes de cada operación
4. **Auditoría**: Cada operación se registra en AuditLog

## Instalación y Configuración

### 1. Ejecutar la Migración SQL
Ejecutar el archivo de migración en la base de datos:
```bash
mysql -u usuario -p nombre_bd < setup/sql/023_crear_tabla_empleados.sql
```

### 2. Compilar el Proyecto
```bash
mvn clean compile
```

### 3. Ejecutar Pruebas
```bash
mvn test
```

### 4. Iniciar la Aplicación
```bash
mvn spring-boot:run
```

## Validaciones de Entrada

### CUIT
- Debe ser obligatorio
- Debe contener exactamente 11 dígitos
- Patrón: `^[0-9]{11}$`

### Nombre
- Debe ser obligatorio
- Longitud: 1-120 caracteres

## Manejo de Errores

La API devuelve los siguientes códigos de error:

| Código | Situación |
|--------|-----------|
| `201 Created` | Empleado creado exitosamente |
| `200 OK` | Operación exitosa (obtener, actualizar) |
| `204 No Content` | Eliminación exitosa |
| `400 Bad Request` | Validación fallida (CUIT o nombre inválido) |
| `401 Unauthorized` | Usuario no autenticado |
| `403 Forbidden` | Usuario sin permiso para la operación |
| `404 Not Found` | Empleado o empresa no encontrada |
| `409 Conflict` | CUIT duplicado en la empresa |

## Auditoría

Todas las operaciones se registran en la tabla `audit_logs` con:
- Tipo de operación (CREACION_EMPLEADO, ACTUALIZACION_EMPLEADO, ELIMINACION_EMPLEADO)
- Usuario que realizó la acción
- CUIT del empleado
- Datos anteriores y nuevos
- Timestamp
- Dirección IP

## Próximas Mejoras Posibles

1. Bulk import de empleados via CSV
2. Reportes de empleados por empresa
3. Historial de cambios por empleado
4. Integración con sistema de nómina
5. Validación de CUIT contra AFIP

