# QA Frontend - Formato Jira

Plantilla para copiar en tickets/subtareas de QA.

## Campos sugeridos por caso

- ID:
- Pantalla:
- Caso de prueba:
- Resultado esperado:
- Evidencia:
- Estado (`OK`/`FAIL`/`BLOQ`):
- Observaciones:

## Casos

### APP

| ID | Pantalla | Caso de prueba | Resultado esperado |
|---|---|---|---|
| APP-001 | `app.html` | Cargar panel principal | Se muestra navbar, sidebar y seccion Panel sin errores visuales |
| APP-002 | `app.html` | Navegar a `Empresas` | Cambia a seccion empresas y mantiene estilos `ui-panel` |
| APP-003 | `app.html` | Navegar a `Lotes` | Tabla carga con estado visual unificado (spinner + estilo) |
| APP-004 | `app.html` | Navegar a `Usuarios` | Tabla carga con estado visual unificado (spinner + estilo) |
| APP-005 | `app.html` | Radicaciones listado vacio | `Sin registros` visible con estilo `ui-state ui-state-muted` |
| APP-006 | `app.html` | Abrir historial de expediente | Historial aparece al seleccionar expediente |
| APP-007 | `app.html` | Error en radicaciones nueva | `#alertaRadicacionNueva` aparece con estilo `ui-state-error` |
| APP-008 | `app.html` | Error en documentacion | `#alertaDocumentoRad` aparece con estilo `ui-state-error` |
| APP-009 | `app.html` | Error en servicios post-radicacion | `#alertaServiciosPostRadicacion` aparece con estilo `ui-state-error` |
| APP-010 | `app.html` | Modal detalle empresa: carga | Se ve `#estadoCargaDetalleEmpresaAdmin` con estilo `ui-state-muted` |
| APP-011 | `app.html` | Modal detalle empresa: error | Se ve `#errorDetalleEmpresaAdmin` con estilo `ui-state-error` |
| APP-012 | `app.html` | Modal detalle lote: error | Se ve `#errorDetalleLote` con estilo `ui-state-error` |
| APP-013 | `app.html` | Alertas de modales de edicion | Alertas de `modalEmpresa`, `modalLote`, `modalUsuario` respetan formato estandar |
| APP-014 | `app.html` | Accesibilidad alertas modales | Alertas anuncian cambios (`role=alert`, `aria-live`) |

### REG

| ID | Pantalla | Caso de prueba | Resultado esperado |
|---|---|---|---|
| REG-001 | `registro.html` | Error de validacion de registro | `#mensajeRegistro` muestra alerta con estilo `ui-state-info/error` segun flujo |
| REG-002 | `registro.html` | Registro exitoso | `#panelPostRegistro` visible con estilo `ui-state-success` |
| REG-003 | `registro.html` | Reenvio de verificacion | Mensajes se muestran sin romper layout del formulario |

### VER

| ID | Pantalla | Caso de prueba | Resultado esperado |
|---|---|---|---|
| VER-001 | `verificar.html` | Carga inicial verificacion | `#estadoVerificacion` muestra `Validando enlace...` con estilo `ui-state-info` |
| VER-002 | `verificar.html` | Token valido | Mensaje final de exito y accion para ingreso visibles |
| VER-003 | `verificar.html` | Token invalido/expirado | Mensaje de error claro y accion de recuperacion visible |

