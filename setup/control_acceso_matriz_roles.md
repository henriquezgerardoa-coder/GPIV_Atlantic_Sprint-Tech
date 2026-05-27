# Matriz de Acceso UI vs Backend

Fecha: 2026-05-26

## Alcance

Este documento resume la correspondencia entre permisos de `frontend` y reglas de `backend` para módulos clave.

Fuente backend principal: `backend/src/main/java/com/gpiv/atlanticsprinttech/backend/configuracion/ConfiguracionSeguridad.java`.

## Resumen por módulo

| Módulo/Sección | Backend | UI esperada |
|---|---|---|
| Empresas (listado) | `ADMINISTRADOR`, `DIRECTIVO`, `SECRETARIO`, `EMPRESA` | Todos esos roles ven listado según su vista |
| Empresas (crear/editar) | `POST /api/empresas`: `ADMINISTRADOR`, `SECRETARIO`, `EMPRESA`; `PUT /api/empresas/*`: idem | UI habilita creación/edición para esos roles |
| Empresas (eliminar) | `DELETE /api/empresas/*`: `ADMINISTRADOR`, `SECRETARIO` | UI solo muestra eliminar para esos roles |
| Lotes (listar) | `GET /api/lotes/**`: `ADMINISTRADOR`, `DIRECTIVO`, `EMPRESA`, `SECRETARIO` | UI visible para esos roles |
| Lotes (crear/editar/eliminar) | `POST/PUT/DELETE /api/lotes/**`: `ADMINISTRADOR`, `SECRETARIO` | UI solo habilita acciones para esos roles |
| Proyectos (listar) | `GET /api/proyectos/**`: `ADMINISTRADOR`, `DIRECTIVO`, `SECRETARIO`, `TECNICO` | UI visible para esos roles |
| Proyectos (crear) | `POST /api/proyectos`: `ADMINISTRADOR`, `SECRETARIO` | UI oculta creación para `TECNICO` y `DIRECTIVO` |
| Proyectos (hitos) | `POST/PATCH/PUT/DELETE hitos`: `ADMINISTRADOR`, `SECRETARIO`, `TECNICO` | UI permite gestionar hitos a esos roles |
| Proyectos (cambiar estado) | `PATCH /api/proyectos/*/estado`: `ADMINISTRADOR`, `SECRETARIO` | UI solo habilita cambio de estado a esos roles |
| Mensajería | `/api/mensajeria/**`: `ADMINISTRADOR`, `DIRECTIVO`, `EMPRESA`, `SECRETARIO` | UI oculta mensajería para `TECNICO` |
| Cambios de rubro (listar) | `GET /api/cambios-rubro`: `ADMINISTRADOR`, `DIRECTIVO` | UI visible para esos roles |
| Cambios de rubro (solicitar) | `POST /api/cambios-rubro`: `EMPRESA` | UI formulario solo para `EMPRESA` |
| Cambios de rubro (resolver) | `PATCH /api/cambios-rubro/*/resolver`: `ADMINISTRADOR`, `DIRECTIVO` | UI resolución solo para esos roles |
| Vinculación inicial empresa | `POST /api/yo/vincular-empresa`: `EMPRESA` | UI debe invocar este endpoint |

## Cambios de alineación aplicados

- `frontend/src/main/resources/static/js/aplicacion.js`
  - Configuración inicial de empresa usa `POST /api/yo/vincular-empresa`.
  - Se restringe navegación de `TECNICO` a secciones no autorizadas por backend.
  - Se oculta menú de mensajería para `TECNICO`.

- `frontend/src/main/resources/static/js/modulos/proyectos.js`
  - Crear proyecto: solo `ADMINISTRADOR` y `SECRETARIO`.
  - Cambiar estado: solo `ADMINISTRADOR` y `SECRETARIO`.
  - Gestión de hitos: `ADMINISTRADOR`, `SECRETARIO`, `TECNICO`.

- `frontend/src/main/resources/static/js/modulos/empresas.js`
  - Edición en tabla: `ADMINISTRADOR`, `SECRETARIO`, `EMPRESA`.
  - Eliminación en tabla: `ADMINISTRADOR`, `SECRETARIO`.

- `frontend/src/main/resources/static/js/modulos/lotes.js`
  - Edición/eliminación en tabla: `ADMINISTRADOR`, `SECRETARIO`.

- `frontend/src/main/resources/static/js/modulos/cambio-rubro.js`
  - Resolución incluye rúbrica obligatoria (4 puntajes + observaciones opcionales) para cumplir contrato backend.

## Riesgos residuales

- Las validaciones de rol en UI son una capa de experiencia de usuario. La seguridad real sigue en backend.
- Se recomienda migrar prompts de rúbrica de `cambio-rubro` a modal con campos explícitos para mejor UX y validación más clara.

