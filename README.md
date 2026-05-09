# GPIV Atlantic Sprint Tech

Este repositorio utiliza Maven multimodulo con un `pom.xml` agregador en la raiz.

## Estructura Maven

- `pom.xml` (raiz): agregador y parent comun
- `backend/pom.xml`: modulo Spring Boot (API y logica servidor)
- `entities/pom.xml`: modulo de entidades y modelo de dominio
- `commons/pom.xml`: contratos de comunicacion compartidos
- `frontend/pom.xml`: modulo cliente/UI (recursos web)

## Estructura de paquetes

- `com.gpiv.atlanticsprinttech`
  - `AplicacionGestionGpiv` permanece en la raiz para conservar el auto-scan de Spring Boot.
- `com.gpiv.atlanticsprinttech.backend`
  - `config`: propiedades e inicializadores de infraestructura.
  - `security`: configuracion de Spring Security, filtros y servicios de contexto/autenticacion.
  - `shared.web`: manejo transversal de errores HTTP.
  - `salud.web`, `catalogo.web`: endpoints transversales simples.
  - `empresa`, `lote`, `radicacion`, `usuario`, `registro`, `estadistica`
    - `web`: controladores REST.
    - `application`: contratos e implementaciones de casos de uso.
    - `persistence`: repositorios JPA.
- `com.gpiv.atlanticsprinttech.entities`
  - `empresa`, `lote`, `radicacion`, `usuario`
- `com.gpiv.atlanticsprinttech.commons`
  - `empresa.dto`, `lote.dto`, `radicacion.dto`, `usuario.dto`, `registro.dto`, `estadistica.dto`, `shared.dto`
- `com.gpiv.atlanticsprinttech.frontend`
  - namespace del modulo frontend

## Convencion de paquetes (limpieza segura)

- `backend`
  - Dominios funcionales: `empresa`, `lote`, `radicacion`, `usuario`, `registro`, `estadistica`, `salud`, `catalogo`.
  - Subcapas estandar cuando aplica:
    - `web`: controladores REST.
    - `application`: casos de uso y servicios de aplicacion.
    - `persistence`: repositorios y acceso a datos.
  - Paquetes transversales reservados:
    - `config`: bootstrap y propiedades.
    - `security`: autenticacion, autorizacion y contexto de usuario.
    - `shared.web`: manejo transversal de errores HTTP.
- `entities`
  - Separacion por dominio (`empresa`, `lote`, `radicacion`, `usuario`).
  - Sin capas tecnicas adicionales para mantener el modelo de dominio simple.
- `commons`
  - Separacion por dominio y subpaquete `dto` (`empresa.dto`, `lote.dto`, `radicacion.dto`, `usuario.dto`, `registro.dto`, `estadistica.dto`, `shared.dto`).
  - Los DTO son contratos compartidos entre backend y frontend.
- `frontend`
  - Namespace del modulo cliente y recursos web en `META-INF/resources`.

Regla de limpieza segura aplicada:

- No se mezclan paquetes antiguos y nuevos para la misma responsabilidad.
- Se evita crear alias paralelos de paquetes (por ejemplo, `controlador` y `web` al mismo tiempo).
- Cualquier nuevo paquete debe seguir la convencion anterior para evitar duplicados semanticos.

Ubicacion actual de recursos de UI:

- `frontend/src/main/resources/META-INF/resources` (`index.html`, `ingreso.html`, `app.html`, `registro.html`, `verificar.html`, `css/`, `js/`, `img/`)

Distribucion actual de clases:

- `backend.empresa.web`
  - `ControladorEmpresa`
- `backend.empresa.application`
  - `ServicioEmpresa`, `ServicioEmpresaImpl`
- `backend.empresa.persistence`
  - `RepositorioEmpresa`
- `backend.salud.web`
  - `ControladorSalud`
- `backend.security`
  - `ConfiguracionSeguridad`, `FiltroLimitacionIngreso`, `ServicioContextoUsuario`, `RegistroIntentosEnMemoria`
- `backend.config`
  - `PropiedadesApiUsuarios`, `PropiedadesRegistroPublico`, `InicializadorUsuariosPredeterminados`, `MigradorConstraintRoles`
- `entities.empresa`
  - `Empresa`
- `entities.lote`
  - `Lote`, `EstadoAsignacionLote`
- `entities.radicacion`
  - `RadicacionSolicitud`, `RadicacionDocumento`, `RadicacionHistorial`, `EstadoRadicacion`, `TipoDocumentoRadicacion`
- `entities.usuario`
  - `Usuario`, `RolUsuario`
- `commons.empresa.dto`
  - `SolicitudEmpresa`, `SolicitudServiciosPostRadicacion`, `SolicitudVehiculoEmpresa`
  - `RespuestaEmpresa`, `RespuestaEmpresaListadoAdmin`, `RespuestaEmpresaDetalleAdmin`, `RespuestaServiciosPostRadicacion`, `RespuestaUsuarioEmpresaAdmin`, `RespuestaVehiculoEmpresa`
- `commons.lote.dto`
  - `SolicitudLote`, `RespuestaLote`
- `commons.radicacion.dto`
  - `SolicitudRadicacion`, `SolicitudCambioEstadoRadicacion`, `SolicitudObservacionRadicacion`, `SolicitudRelevamientoPedidoLotes`
  - `RespuestaRadicacion`, `RespuestaDocumentoRadicacion`, `RespuestaHistorialRadicacion`
- `commons.usuario.dto`
  - `SolicitudUsuario`, `SolicitudActualizacionUsuario`, `SolicitudActualizacionPerfil`, `SolicitudCambioClave`, `SolicitudRestablecerClave`
  - `RespuestaUsuario`, `RespuestaYo`
- `commons.registro.dto`
  - `SolicitudRegistroPublico`, `SolicitudReenvioVerificacion`
- `commons.estadistica.dto`
  - `RespuestaEstadisticas`
- `commons.shared.dto`
  - `RespuestaOperacion`

Dependencias entre paquetes:

- `backend` depende de `entities` y `commons`
- `commons` comparte contratos de comunicacion entre frontend y backend
- `frontend` contiene los estaticos web y puede consumir contratos desde `commons`
- `entities` no depende de los demas paquetes

## Gestion de usuarios y roles

Roles disponibles:

- `ADMINISTRADOR`
- `DIRECTIVO`
- `EMPRESA`

Usuarios predeterminados (solo para entorno inicial/local):

- `admin` / `admin12345` (`ADMINISTRADOR`)
- `directivo` / `directivo123` (`DIRECTIVO`)
- `empresa` / `empresa12345` (`EMPRESA`)

Registro publico con verificacion por correo:

- Pantalla de registro: `GET /registro.html`
- Verificacion por token: `GET /verificar.html?token=...`
- API registro: `POST /api/public/registro`
- API verificar correo: `GET /api/public/verificacion?token=...`
- API reenvio: `POST /api/public/verificacion/reenviar`

El login acepta usuario o correo verificado.

Estas credenciales se configuran mediante `app.api.usuarios.semilla.*` y se centralizan en `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/config/PropiedadesApiUsuarios.java`.

Permisos por rol:

- `GET /api/empresas/**`: `ADMINISTRADOR`, `DIRECTIVO`, `EMPRESA`
- `GET /api/empresas/admin/vista` y `GET /api/empresas/admin/vista/{id}`: solo `ADMINISTRADOR` (panel de visualizacion por nombre + detalle)
- `POST /api/empresas`: `ADMINISTRADOR`, `EMPRESA` (registro inicial de su empresa)
- `PUT /api/empresas/{id}`: `ADMINISTRADOR`, `EMPRESA` (solo su empresa asignada)
- `DELETE /api/empresas/**`: solo `ADMINISTRADOR`
- `GET /api/lotes/**`: `ADMINISTRADOR`, `DIRECTIVO`, `EMPRESA`
- `POST/PUT/DELETE /api/lotes/**`: `ADMINISTRADOR`, `DIRECTIVO`
- `GET/POST/PUT/PATCH/DELETE /api/usuarios/**`: solo `ADMINISTRADOR`
- `GET /api/catalogos/roles`: cualquier usuario autenticado
- `GET /api/usuarios/roles`: alias temporal legado (solo `ADMINISTRADOR`)
- `PATCH /api/usuarios/mi-clave`: cualquier usuario autenticado
- `PATCH /api/yo/perfil`: cualquier usuario autenticado (datos personales)

Regla de aislamiento para `EMPRESA`: solo puede consultar la empresa y los lotes de su empresa asignada.

Restricciones por ciclo de vida de radicacion:

- Cuando una empresa tiene expediente en estado `RADICADA`, no puede editar datos generales de empresa.
- En estado `RADICADA`, se habilitan servicios post-radicacion para actualizar empleados y vehiculos.

### Control de acceso por etapas para `EMPRESA`

Se documento el diseno funcional y tecnico de control por ciclo de vida (`REGISTRADA`, `AUTORIZADA`, `RADICADA`) en:

- `setup/control_acceso_empresa_etapas.md`

Este diseno define matriz de permisos por etapa, flags de base de datos propuestos, transiciones validas, flujo de aprobacion Admin/Directivo y comportamiento de errores (`401/403/404/409`).

Cabeceras de deprecacion en `GET /api/usuarios/roles`:

- `Deprecation: configurable por app.api.usuarios.roles-legado.deprecation / USUARIOS_ROLES_LEGADO_DEPRECATION`
- `Sunset: configurable por app.api.usuarios.roles-legado.sunset / USUARIOS_ROLES_LEGADO_SUNSET`
- `Link: configurable por app.api.usuarios.roles-legado.link / USUARIOS_ROLES_LEGADO_LINK`

Estas propiedades se centralizan en `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/config/PropiedadesApiUsuarios.java`.

Endpoints de usuarios:

- `GET /api/usuarios`
- `GET /api/catalogos/roles`
- `GET /api/usuarios/roles` (alias temporal legado)
- `GET /api/usuarios/{id}`
- `POST /api/usuarios`
- `PUT /api/usuarios/{id}`
- `PATCH /api/usuarios/{id}/clave`
- `PATCH /api/usuarios/mi-clave`
- `DELETE /api/usuarios/{id}`

Endpoint de perfil propio:

- `GET /api/yo`
- `PATCH /api/yo/perfil`

Ejemplo de cuerpo para `POST /api/usuarios`:

```json
{
  "nombreUsuario": "jlopez",
  "nombreCompleto": "Juan Lopez",
  "clave": "clave12345",
  "activo": true,
  "roles": ["EMPRESA"]
}
```

Validaciones de build:

- El `pom.xml` raiz ejecuta `maven-enforcer-plugin`.
- Requiere Maven `3.9+` y Java `17+`.

## Verificacion rapida

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
mvn -q validate
```

## Ejecutar aplicacion

Variables opcionales para PostgreSQL (perfil `dev` por defecto):

```bash
export DB_URL='jdbc:postgresql://localhost:5432/gpiv'
export DB_USER='admin'
export DB_PASSWORD='password123'
export SERVER_PORT='8090'
export SEED_ADMIN_CLAVE='admin12345'
export SEED_DIRECTIVO_CLAVE='directivo123'
export SEED_EMPRESA_CLAVE='empresa12345'
export USUARIOS_ROLES_LEGADO_DEPRECATION='true'
export USUARIOS_ROLES_LEGADO_SUNSET='Thu, 31 Dec 2026 23:59:59 GMT'
export USUARIOS_ROLES_LEGADO_LINK='</api/catalogos/roles>; rel="successor-version"'
export REGISTRO_URL_BASE_VERIFICACION='http://localhost:8090/verificar.html'
export REGISTRO_CONTACTO_SOPORTE='soporte@gpiv.local'
export REGISTRO_TOKEN_EXPIRACION_HORAS='24'
export REGISTRO_MAIL_HABILITADO='false'
export REGISTRO_MAIL_REMITENTE='no-reply@gpiv.local'
export REGISTRO_MAX_INTENTOS='5'
export REGISTRO_MAX_REENVIOS='5'
export INGRESO_MAX_INTENTOS='5'
export SEGURIDAD_VENTANA_MINUTOS='15'
```

El prefijo comun de configuracion es `app.api.usuarios.*` (`semilla.*` y `roles-legado.*`).

Comando recomendado (instancia unica en `8090`):

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
bash setup/backend_unica_8090.sh
```

Alternativa manual (si no usas el script):

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
mvn -q -DskipTests install
mvn -pl backend spring-boot:run
```

> Si arrancas solo con `-pl backend` y hay cambios recientes en `commons` o `entities`, puede fallar con `NoClassDefFoundError` por artefactos desactualizados.

Endpoint disponible:

- Base local por defecto: `http://localhost:8090`
- `GET /salud` -> `{"estado":"ok"}`
- `GET /health` -> alias temporal compatible con la ruta anterior

### Migracion de roles a `DIRECTIVO`

Si tu base ya tiene usuarios con el rol anterior `OPERADOR`, ejecuta una vez:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
DB_CONTAINER='gisto-db' DB_NAME='gpiv' DB_USER='admin' bash setup/migrar_roles_directivo.sh
```

El SQL aplicado queda en `setup/sql/003_renombrar_roles_directivo.sql`.

### Migracion de esquema de `empresas`

Si tu base tiene un esquema anterior de `empresas` (sin campos como `actividad_economica`, `fecha_registro`, etc.), ejecuta una vez:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
DB_CONTAINER='gisto-db' DB_NAME='gpiv' DB_USER='admin' bash setup/migrar_empresas_schema.sh
```

El SQL aplicado queda en `setup/sql/004_alinear_tabla_empresas.sql`.

### Migracion de lotes sin empresa asignada

Si deseas crear lotes sin empresa inicial (asignacion posterior por expediente), ejecuta una vez:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
DB_CONTAINER='gisto-db' DB_NAME='gpiv' DB_USER='admin' bash setup/migrar_lotes_sin_empresa.sh
```

El SQL aplicado queda en `setup/sql/005_habilitar_lotes_sin_empresa.sql`.

### Migracion de detalle de asignacion en lotes

Para agregar campos de detalle (`fecha_asignacion`, `estado_asignacion`, `numero_expediente_referencia`), ejecuta una vez:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
DB_CONTAINER='gisto-db' DB_NAME='gpiv' DB_USER='admin' bash setup/migrar_detalle_asignacion_lotes.sh
```

El SQL aplicado queda en `setup/sql/006_detalle_asignacion_lotes.sql`.

### Migracion de columna espacial `poligono` en lotes (PostGIS)

Para habilitar persistencia real de geometria en lotes (`geometry(Polygon,4326)`), ejecuta una vez:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
DB_CONTAINER='gisto-db' DB_NAME='gpiv' DB_USER='admin' bash setup/migrar_poligono_lotes.sh
```

El SQL aplicado queda en `setup/sql/008_habilitar_poligono_lotes.sql`.

### Desasignar todos los lotes (empresa en blanco / "Sin asignar")

Si necesitas dejar todos los lotes sin empresa asignada (asignacion posterior por expediente), ejecuta:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
DB_CONTAINER='gisto-db' DB_NAME='gpiv' DB_USER='admin' bash setup/desasignar_todos_lotes.sh
```

El SQL aplicado queda en `setup/sql/007_desasignar_todos_lotes.sql`.

CRUD inicial de empresas:

- `GET /api/empresas`
- `GET /api/empresas/{id}`
- `POST /api/empresas`
- `PUT /api/empresas/{id}`
- `DELETE /api/empresas/{id}`

Servicios post-radicacion (habilitados con expediente `RADICADA`):

- `GET /api/empresas/{id}/servicios-post-radicacion`
- `PATCH /api/empresas/{id}/servicios-post-radicacion`

Permisos de servicios post-radicacion:

- `GET /api/empresas/{id}/servicios-post-radicacion`: `ADMINISTRADOR`, `DIRECTIVO`, `EMPRESA`
- `PATCH /api/empresas/{id}/servicios-post-radicacion`: `ADMINISTRADOR`, `EMPRESA`

Panel de visualizacion ADMIN de empresas:

- `GET /api/empresas/admin/vista` (listado por nombre y total)
- `GET /api/empresas/admin/vista/{id}` (detalle completo de empresa, usuario asociado, fecha de registro, status, empleados y vehiculos)

CRUD inicial de lotes:

- `GET /api/lotes`
- `GET /api/lotes/{id}`
- `POST /api/lotes`
- `PUT /api/lotes/{id}`
- `DELETE /api/lotes/{id}`

Notas de negocio para lotes:

- Un lote puede crearse sin empresa asignada (`empresaId: null`) y asignarse posteriormente.

Ejemplo de cuerpo para `POST`/`PUT` de lotes:

```json
{
  "codigo": "L-01",
  "superficieMetrosCuadrados": 350.5,
  "ocupado": false,
  "empresaId": 1
}
```

Ejemplo de lote sin empresa asignada:

```json
{
  "codigo": "L-LIB-01",
  "superficieMetrosCuadrados": 5000,
  "ocupado": false,
  "empresaId": null
}
```

Ejemplo de cuerpo para `POST /api/radicaciones` (empresa):

```json
{
  "tipoSolicitud": "Servicio de agua",
  "descripcion": "Solicitud de conexion para nave 3",
  "usoEstimativo": "350 m3/mes"
}
```

Ejemplo de cuerpo para `POST`/`PUT` de empresas:

```json
{
  "nombre": "Empresa Uno",
  "razonSocial": "Empresa Uno SA",
  "nit": "NIT-EMP-001",
  "cuit": "20-12345678-9",
  "direccion": "Av. Industrial 123",
  "actividadEconomica": "Metalurgica",
  "correoElectronico": "contacto@empresa.com",
  "telefono": "2990000000"
}
```

Ejemplo de cuerpo para servicios post-radicacion:

```json
{
  "cantidadEmpleados": 42,
  "vehiculos": [
    { "placa": "ABC123", "tipo": "CAMION", "descripcion": "Carga" },
    { "placa": "DEF456", "tipo": "AUTO", "descripcion": "Traslado interno" }
  ]
}
```

## Ejecutar pruebas

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
mvn test
```

El perfil `dev` usa PostgreSQL (`backend/src/main/resources/application-dev.properties`) y toma credenciales desde variables de entorno.

En pruebas se usa `H2` en memoria (`backend/src/test/resources/application-test.properties`) para evitar dependencia de una base local.

## Criterios de codigo aplicados

- Nombres de clases, metodos, atributos y variables en español.
- Inyeccion de dependencias por constructor.
- Encapsulamiento en entidades, exponiendo solo comportamiento necesario.
- Servicio mediante interfaz para mantener polimorfismo sin sobreingenieria.

## Variante desktop (Electron)

Se agrego un wrapper desktop en `desktop/` para ejecutar GPIV como aplicacion de escritorio reutilizando el backend Spring Boot actual.

Inicio rapido:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
npm install
npm run dev
```

Empaquetado Linux:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
npm run dist:linux
```

Mas detalle en `desktop/README.md`.

Comando rapido para reiniciar backend local en `8090` y validar headers de iframe (`SAMEORIGIN` + CSP):

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech
bash setup/restart_backend.sh
```
