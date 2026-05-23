# 📋 Explicación de la Implementación - Sistema de Empleados

## 🎯 Requisito Implementado

**"Cada empresa debe cargar CUIT y nombre de sus empleados pudiendo ver el listado general de todos sus empleados, pero ADMIN y DIRECTIVO solo ven la cantidad de empleados cargados"**

---

## 📦 Componentes Creados

### 1. **Entidad: Empleado** 
📁 `entities/src/main/java/com/gpiv/atlanticsprinttech/entities/dominio/Empleado.java`

```java
@Entity
@Table(name = "empleados")
public class Empleado {
    Long id              // ID único autogenerado
    String cuit          // CUIT del empleado (11 dígitos)
    String nombre        // Nombre del empleado
    Empresa empresa      // Referencias a la empresa propietaria
    LocalDateTime fechaRegistro  // Cuándo se registró
}
```

**¿Qué hace?**
- Define la estructura de datos de un empleado
- Se relaciona con Empresa (un empleado pertenece a una empresa)
- Garantiza que no haya CUITs duplicados dentro de la misma empresa

---

### 2. **Repositorio: RepositorioEmpleado**
📁 `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/repositorio/RepositorioEmpleado.java`

**¿Qué hace?**
- Accede a la base de datos para guardar/recuperar empleados
- Métodos principales:
  - `findByEmpresaId()` - Obtiene todos los empleados de una empresa
  - `countByEmpresaId()` - Cuenta empleados (para ADMIN/DIRECTIVO)
  - `existsByEmpresaIdAndCuit()` - Verifica CUIT duplicado

---

### 3. **DTOs (Data Transfer Objects)**

#### 📁 `SolicitudEmpleado.java`
```json
{
  "cuit": "20123456789",    // Obligatorio, 11 dígitos
  "nombre": "Juan Pérez"     // Obligatorio, 1-120 caracteres
}
```
**¿Qué hace?** Define qué datos envía el cliente al crear/actualizar

#### 📁 `RespuestaEmpleado.java`
```json
{
  "id": 1,
  "cuit": "20123456789",
  "nombre": "Juan Pérez",
  "fechaRegistro": "2024-05-22T10:30:00"
}
```
**¿Qué hace?** Devuelve los datos completos del empleado (solo EMPRESA)

#### 📁 `RespuestaEmpleadosCantidad.java`
```json
{
  "empresaId": 1,
  "nombreEmpresa": "Mi Empresa S.A.",
  "cantidadEmpleados": 15
}
```
**¿Qué hace?** Devuelve SOLO la cantidad (solo ADMIN/DIRECTIVO)

---

### 4. **Servicio: ServicioEmpleado**

**Interfaz** 📁 `ServicioEmpleado.java`
Define qué operaciones se pueden hacer (contrato)

**Implementación** 📁 `ServicioEmpleadoImpl.java`

```java
public class ServicioEmpleadoImpl implements ServicioEmpleado {
    
    // Crear empleado (solo EMPRESA)
    public Empleado crear(Long empresaId, SolicitudEmpleado solicitud, String usuario) {
        1. Verifica que el usuario sea EMPRESA
        2. Verifica que sea su propia empresa
        3. Valida CUIT no duplicado
        4. Guarda en BD
        5. Registra en auditoría
    }
    
    // Listar todos los empleados (solo EMPRESA)
    public List<RespuestaEmpleado> listarPorEmpresa(Long empresaId, String usuario) {
        1. Verifica acceso
        2. Obtiene listado completo
        3. Retorna detalles completos
    }
    
    // Ver SOLO cantidad (solo ADMIN/DIRECTIVO)
    public RespuestaEmpleadosCantidad obtenerCantidadPorEmpresa(Long empresaId, String usuario) {
        1. Verifica que sea ADMIN o DIRECTIVO
        2. Cuenta empleados
        3. Retorna SOLO la cantidad (sin detalles)
    }
    
    // Actualizar (solo EMPRESA)
    public Empleado actualizar(...) { ... }
    
    // Eliminar (solo EMPRESA)
    public void eliminar(...) { ... }
}
```

**¿Qué hace?**
- Contiene la lógica de negocio
- Valida permisos según el rol del usuario
- Valida que cada empresa solo acceda a sus datos
- Registra todas las operaciones en auditoría

---

### 5. **Controlador: ControladorEmpleado**
📁 `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/controlador/ControladorEmpleado.java`

```java
@RestController
@RequestMapping("/api/empresas/{empresaId}/empleados")
public class ControladorEmpleado {
    
    @PostMapping
    public ResponseEntity<RespuestaEmpleado> crear(...) {
        // Crea un empleado
        // Solo EMPRESA puede acceder
    }
    
    @GetMapping
    public List<RespuestaEmpleado> listar(...) {
        // Lista todos los empleados de la empresa
        // Solo EMPRESA ve detalles
    }
    
    @GetMapping("/cantidad")
    public RespuestaEmpleadosCantidad obtenerCantidad(...) {
        // Ve SOLO la cantidad
        // Solo ADMIN y DIRECTIVO pueden acceder
    }
    
    @PutMapping("/{empleadoId}")
    public RespuestaEmpleado actualizar(...) {
        // Actualiza empleado
        // Solo EMPRESA puede
    }
    
    @DeleteMapping("/{empleadoId}")
    public ResponseEntity<Void> eliminar(...) {
        // Elimina empleado
        // Solo EMPRESA puede
    }
}
```

**¿Qué hace?**
- Define los endpoints HTTP
- Recibe las peticiones del cliente
- Llama al servicio
- Devuelve respuestas formateadas

---

### 6. **Pruebas: ServicioEmpleadoImplTest.java**
📁 `backend/src/test/java/.../ServicioEmpleadoImplTest.java`

**Pruebas implementadas:**
- ✓ Crear empleado exitosamente
- ✓ Error si no es EMPRESA
- ✓ Error si es de otra empresa
- ✓ Error si CUIT está duplicado
- ✓ Obtener cantidad como ADMIN
- ✓ Error si EMPRESA intenta ver cantidad

**¿Qué hace?**
Verifica que la lógica funcione correctamente en casos normales y de error

---

### 7. **Base de Datos: 023_crear_tabla_empleados.sql**
📁 `setup/sql/023_crear_tabla_empleados.sql`

```sql
CREATE TABLE empleados (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cuit VARCHAR(20) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    empresa_id BIGINT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    UNIQUE KEY (empresa_id, cuit),  -- No hay CUITs duplicados por empresa
    FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);
```

**¿Qué hace?**
- Define la tabla en la base de datos
- Garantiza integridad de datos
- Índices para búsquedas rápidas

---

## 🔄 Flujo de Operaciones

### Crear Empleado (EMPRESA)
```
1. EMPRESA envía: POST /api/empresas/1/empleados
   {
     "cuit": "20123456789",
     "nombre": "Juan Pérez"
   }

2. ControladorEmpleado.crear() intercepta la petición

3. Llama a ServicioEmpleadoImpl.crear() que:
   ✓ Obtiene el usuario autenticado
   ✓ Verifica que sea EMPRESA
   ✓ Verifica que sea su empresa (empresaId=1)
   ✓ Verifica CUIT no duplicado
   ✓ Crea el Empleado en BD vía RepositorioEmpleado
   ✓ Registra en AuditLog

4. Retorna: 201 Created con RespuestaEmpleado completa
   {
     "id": 1,
     "cuit": "20123456789",
     "nombre": "Juan Pérez",
     "fechaRegistro": "2024-05-22T10:30:00"
   }
```

### Listar Empleados (EMPRESA)
```
1. EMPRESA envía: GET /api/empresas/1/empleados

2. ControladorEmpleado.listar() intercepta

3. ServicioEmpleadoImpl.listarPorEmpresa() que:
   ✓ Obtiene el usuario
   ✓ Verifica acceso
   ✓ Obtiene lista del repositorio
   ✓ Retorna detalles COMPLETOS de cada empleado

4. Retorna: 200 OK con lista de RespuestaEmpleado
   [
     {
       "id": 1,
       "cuit": "20123456789",
       "nombre": "Juan Pérez",
       "fechaRegistro": "2024-05-22T10:30:00"
     },
     ...
   ]
```

### Ver Cantidad (ADMIN/DIRECTIVO)
```
1. ADMIN envía: GET /api/empresas/1/empleados/cantidad

2. ControladorEmpleado.obtenerCantidad() intercepta

3. ServicioEmpleadoImpl.obtenerCantidadPorEmpresa() que:
   ✓ Obtiene el usuario
   ✓ VERIFICA que sea ADMIN o DIRECTIVO
   ✓ Cuenta empleados del repositorio
   ✓ Retorna detalles MÍNIMOS (solo cantidad)

4. Retorna: 200 OK SIN detalles de empleados
   {
     "empresaId": 1,
     "nombreEmpresa": "Mi Empresa S.A.",
     "cantidadEmpleados": 1
   }

NOTA: Si EMPRESA intenta acceder, retorna 403 Forbidden
```

---

## 🔐 Matriz de Permisos

| Endpoint | EMPRESA | ADMIN | DIRECTIVO |
|----------|---------|-------|-----------|
| POST crear | ✅ Acceso | ❌ 403 | ❌ 403 |
| GET listar | ✅ Ve detalles | ❌ 403 | ❌ 403 |
| GET detalles | ✅ Ve detalles | ❌ 403 | ❌ 403 |
| GET cantidad | ❌ 403 | ✅ Solo cantidad | ✅ Solo cantidad |
| PUT actualizar | ✅ Acceso | ❌ 403 | ❌ 403 |
| DELETE eliminar | ✅ Acceso | ❌ 403 | ❌ 403 |

---

## ✅ Validaciones Implementadas

### Por Rol
- ✓ Solo EMPRESA puede crear/modificar/eliminar
- ✓ Solo ADMIN/DIRECTIVO pueden ver cantidad
- ✓ Otros roles rechazados

### Por Empresa
- ✓ EMPRESA solo opera con su empresa
- ✓ No puede acceder a datos de otras empresas

### Por Formato
- ✓ CUIT: Exactamente 11 dígitos (patrón: ^[0-9]{11}$)
- ✓ Nombre: 1-120 caracteres
- ✓ CUIT único por empresa

### Por Integridad
- ✓ No permite CUITs duplicados
- ✓ Valida relaciones con Empresa
- ✓ Registra auditoría

---

## 📊 Códigos HTTP Devueltos

| Código | Situación | Ejemplo |
|--------|-----------|---------|
| 201 | Created exitosamente | POST /empleados → 201 |
| 200 | OK | GET /empleados → 200 |
| 204 | No Content (DELETE) | DELETE /empleados/1 → 204 |
| 400 | Bad Request (validación) | CUIT no es 11 dígitos → 400 |
| 401 | Unauthorized | Sin token JWT → 401 |
| 403 | Forbidden | EMPRESA intenta ver cantidad → 403 |
| 404 | Not Found | Empleado no existe → 404 |
| 409 | Conflict | CUIT duplicado → 409 |

---

## 🔍 Auditoría

Cada operación se registra en `audit_logs`:

```
CREACION_EMPLEADO:
  - Usuario: "empresa_user"
  - CUIT: "20123456789"
  - Anterior: null
  - Nuevo: "Empleado: Juan Pérez | CUIT=20123456789"
  - IP: "192.168.1.100"
  - Timestamp: 2024-05-22 10:30:00

ACTUALIZACION_EMPLEADO:
  - Usuario: "empresa_user"
  - CUIT: "20123456789"
  - Anterior: "Empleado: Juan Pérez | CUIT=20123456789"
  - Nuevo: "Empleado: Juan P. García | CUIT=20123456789"
  - IP: "192.168.1.100"
  - Timestamp: 2024-05-22 10:35:00

ELIMINACION_EMPLEADO:
  - Usuario: "empresa_user"
  - CUIT: "20123456789"
  - Anterior: "Empleado: Juan P. García | CUIT=20123456789"
  - Nuevo: null
  - IP: "192.168.1.100"
  - Timestamp: 2024-05-22 10:40:00
```

---

## 🚀 Flujo Completo de Uso

```mermaid
EMPRESA quiere agregar empleados
    ↓
POST /api/empresas/1/empleados
    ↓
ControladorEmpleado intercepta
    ↓
ServicioEmpleado.crear() valida:
  - ¿Es EMPRESA? → Sí
  - ¿Es su empresa? → Sí
  - ¿CUIT duplicado? → No
    ↓
RepositorioEmpleado.save() guarda
    ↓
AuditLog.registrar() audita
    ↓
Retorna 201 Created con detalles
```

```mermaid
ADMIN quiere ver cantidad de empleados
    ↓
GET /api/empresas/1/empleados/cantidad
    ↓
ControladorEmpleado intercepta
    ↓
ServicioEmpleado.obtenerCantidad() valida:
  - ¿Es ADMIN o DIRECTIVO? → Sí (es ADMIN)
    ↓
RepositorioEmpleado.countByEmpresaId() cuenta
    ↓
Retorna 200 OK con SOLO cantidad:
  {
    "empresaId": 1,
    "nombreEmpresa": "...",
    "cantidadEmpleados": 42
  }
```

```mermaid
EMPRESA intenta ver cantidad (PROHIBIDO)
    ↓
GET /api/empresas/1/empleados/cantidad
    ↓
ControladorEmpleado intercepta
    ↓
ServicioEmpleado.obtenerCantidad() valida:
  - ¿Es ADMIN o DIRECTIVO? → No (es EMPRESA)
    ↓
Lanza ResponseStatusException(403)
    ↓
Retorna 403 Forbidden
  "Solo ADMIN y DIRECTIVO pueden acceder..."
```

---

## 📚 Archivos del Proyecto

```
GPIV_Atlantic_Sprint-Tech/
├── entities/
│   └── dominio/
│       └── Empleado.java ........................ Entidad
├── backend/
│   ├── repositorio/
│   │   └── RepositorioEmpleado.java ............ Acceso datos
│   ├── servicio/
│   │   ├── ServicioEmpleado.java .............. Interfaz
│   │   └── implementacion/
│   │       └── ServicioEmpleadoImpl.java ....... Lógica
│   ├── controlador/
│   │   └── ControladorEmpleado.java ........... REST API
│   └── test/
│       └── ServicioEmpleadoImplTest.java ...... Pruebas
├── commons/
│   └── comunicacion/dto/
│       ├── SolicitudEmpleado.java ............. Input DTO
│       ├── RespuestaEmpleado.java ............. Output DTO
│       └── RespuestaEmpleadosCantidad.java .... Output cantidad
├── setup/
│   ├── sql/
│   │   └── 023_crear_tabla_empleados.sql ..... Migración BD
│   └── ejemplos_empleados_curl.sh ............ Ejemplos
├── EMPLEADOS_FEATURE.md ....................... Docs detallada
├── IMPLEMENTACION_EMPLEADOS_RESUMEN.md ....... Resumen
├── INICIO_RAPIDO_EMPLEADOS.md ................ Guía rápida
└── COMPONENTES_DETALLADOS.md ................. ← Este archivo
```

---

## ✨ Características Implementadas

✅ **Crear empleados**: EMPRESA carga CUIT y nombre  
✅ **Ver listado completo**: EMPRESA ve todos sus empleados  
✅ **Ver cantidad**: ADMIN/DIRECTIVO ven SOLO cantidad  
✅ **Actualizar**: EMPRESA puede editar sus empleados  
✅ **Eliminar**: EMPRESA puede eliminar sus empleados  
✅ **Auditoría**: Todas las operaciones se registran  
✅ **Validaciones**: CUIT (11 dígitos), nombre, duplicados  
✅ **Control de acceso**: Por rol y por empresa  
✅ **Pruebas unitarias**: Casos principales cubiertos  
✅ **Documentación**: Completa y con ejemplos  

---

**Todo compilado exitosamente ✅**

