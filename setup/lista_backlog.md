# Backlog de Issues para CSyD El Cóndor - Sistema de Gestión

Este archivo es la fuente de verdad para la automatización de la creación de Issues.
NO editar la estructura de las tablas, solo añadir o modificar filas.

### Definiciones de Metadatos
- **Tipo:** `requerimiento` (R) | `historia-de-usuario` (HU)
- **Prioridad:** `Alta` | `Media` | `Baja`
- **Módulo:** `catastro`, `adjudicacion`, `fiscalizacion`, `no-funcional`, `informes`.

---

## Tabla de Requerimientos

| ID | Título (Resumen) | Descripción Detallada | Tipo | Prioridad | Módulo |
|:---|:---|:---|:---|:---|:---|
| R01 | Inventario de Lotes | Gestionar inventario diferenciado: "Parque Viejo" (63 lotes de 5000 m2) y "Parque Nuevo" (60 lotes de 1200, 2500, 5000 m2). Incluir Nomenclatura Catastral y dimensiones. | requerimiento | Alta | catastro |
| R02 | Visualización Geográfica | Visualización por ubicación (Ruta 'Los Chicos', Vía 'Los Grandes') y filtro por estado: Disponible, Pre-adjudicado, Ocupado/Escriturado. | requerimiento | Alta | catastro |
| R03 | Registro de Servicios | Monitoreo disponibilidad agua cruda (estación bombeo) y servicios generales (Luz, Gas, Internet). | requerimiento | Media | catastro |
| R04 | Gestión de Solicitudes | Integración desde Google Forms para nuevos interesados. Carga del Proyecto Productivo (materia prima, impacto ambiental, rentabilidad). | requerimiento | Alta | adjudicacion |
| R05 | Flujo por etapas | Etapa 1: Empleabilidad/Materia Prima/Reciclaje. Etapa 2: Financiero (Rentabilidad). Etapa 3: Preadjudicación y cronograma de obra. | requerimiento | Alta | adjudicacion |
| R06 | Resolución de Solicitudes | Registro decisión formal (Aprobación/Rechazo). Si es rechazada, obligatorio registrar motivo para trazabilidad legal. | requerimiento | Alta | adjudicacion |
| R07 | Registro de Rúbricas | Adjuntar Acta de reunión digitalizada (PDF) con firmas de los 8 directivos para validez legal. | requerimiento | Alta | adjudicacion |
| R08 | Control de Desadjudicación | Alertas automáticas ante incumplimiento de cronograma de obra para iniciar reversión. | requerimiento | Alta | adjudicacion |
| R09 | Padrón de empresas | Registro activo y autogestionado por la empresa (datos contacto, representante legal). | requerimiento | Media | fiscalizacion |
| R10 | Censo empleados/flota | Registro permanente personal y vehículos para exenciones impositivas (Ingresos brutos). | requerimiento | Media | fiscalizacion |
| R11 | Control de rubro | Bloqueo cambios actividad sin autorización. Registro ampliaciones internos para escriturados. | requerimiento | Media | fiscalizacion |
| R12 | Trazabilidad Digital | Trazabilidad completa de documento, eliminando expedientes físicos (Ley digitalización). | requerimiento | Alta | no-funcional |
| R13 | Seguridad Multimodal | Acceso: Google Auth (Gmail) para empleados/directivos. Recuperación automatizada por mail. | requerimiento | Alta | no-funcional |
| R14 | Control Acceso (Roles) | Admin: Total. Legislativo: Lectura. Empresas: Limitado a perfil/solicitudes. | requerimiento | Alta | no-funcional |
| R15 | Escalabilidad | Arquitectura permita futuro módulo cobro expensas/servicios comunes (balanza, sampi, bomberos). | requerimiento | Baja | no-funcional |
| R16 | Dashboard Ocupación | Porcentaje adjudicados/disponibles con mapa de calor. | requerimiento | Media | informes |
| R17 | Reporte Empleabilidad | Total trabajadores activos (impacto socioeconómico). | requerimiento | Media | informes |
| R18 | Monitor Trazabilidad | Historial desde Google Form inicial hasta escrituración. | requerimiento | Alta | informes |
| R19 | Alarmas Vencimientos | Proyectos pre-adjudicados sin avances de obra según cronograma. | requerimiento | Alta | informes |

---

## Tabla de Historias de Usuario

| ID | Título (Resumen) | Descripción (Como... quiero... para...) | Requerimientos Vinculados | Tipo | Prioridad | Módulo | Criterios de Aceptación |
|:---|:---|:---|:---|:---|:---|:---|:---|
| HU-01 | Visualización/Gestión de Lotes | Como Administrador de Catastro, quiero visualizar los lotes en un mapa interactivo para gestionar su disponibilidad eficientemente. | R01, R02 | historia-de-usuario | Alta | catastro | - [ ] Mapa muestra lotes.<br>- [ ] Filtro por estado.<br>- [ ] Click en lote muestra Nomenclatura. |
| HU-02 | Registrar Solicitud Radicación | Como Nuevo Interesado, quiero registrar mi solicitud vía web para iniciar el trámite de radicación en el Parque. | R04 | historia-de-usuario | Alta | adjudicacion | - [ ] Carga desde Google Forms.<br>- [ ] Subida de PDF Proyecto Productivo. |
| HU-03 | Evaluación por Etapas | Como Directivo, quiero evaluar las solicitudes en etapas financieras y productivas para decidir la adjudicación. | R05 | historia-de-usuario | Alta | adjudicacion | - [ ] Vistas separadas por Etapa.<br>- [ ] Formulario de puntuación. |
| HU-04 | Resolución y Dictamen | Como Directivo, quiero registrar el dictamen formal de una solicitud para dar cierre legal al proceso. | R06 | historia-de-usuario | Alta | adjudicacion | - [ ] Botón Aprobar/Rechazar.<br>- [ ] Campo 'Motivo Rechazo' obligatorio. |
| HU-05 | Registro de Rúbricas | Como Secretario, quiero subir el Acta de reunión digitalizada con las firmas para validar legalmente la adjudicación. | R07 | historia-de-usuario | Alta | adjudicacion | - [ ] Upload de PDF.<br>- [ ] Visualización de PDF en Issue. |
| HU-06 | Perfil de Empresas | Como Representante Legal de Empresa, quiero actualizar los datos de mi empresa de forma autogestionada para mantener la información al día. | R09 | historia-de-usuario | Media | fiscalizacion | - [ ] Panel de autogestión.<br>- [ ] Validación de CUIT. |
| HU-07 | Censo vehicular/personal | Como Empresa Radicada, quiero registrar mi personal y flota vehicular para aplicar a exenciones impositivas. | R10 | historia-de-usuario | Media | fiscalizacion | - [ ] Formulario carga personal (registrado/no).<br>- [ ] Carga de patentes. |
| HU-08 | Solicitud cambio rubro | Como Empresa Escriturada, quiero solicitar cambios de actividad o ampliaciones para recibir autorización formal. | R11 | historia-de-usuario | Media | fiscalizacion | - [ ] Formulario solicitud ampliación.<br>- [ ] Bloqueo de cambios sin OK. |
| HU-09 | Alertas incumplimiento plazos | Como Administrador, quiero recibir alertas ante el incumplimiento de plazos de obra para iniciar reversiones de lotes. | R08, R19 | historia-de-usuario | Alta | adjudicacion | - [ ] Alertas automáticas.<br>- [ ] Integración con cronograma R05. |
| HU-10 | Monitor trazabilidad | Como Auditor Legislativo, quiero visualizar la trazabilidad completa del expediente digital para garantizar transparencia. | R12, R18 | historia-de-usuario | Alta | no-funcional | - [ ] Línea de tiempo del expediente.<br>- [ ] Enlaces a documentos firmados. |
| HU-11 | Dashboard gerencial | Como Directivo, quiero visualizar un dashboard de ocupación y empleabilidad para la toma de decisiones estratégicas. | R16, R17 | historia-de-usuario | Media | informes | - [ ] Gráfico torta ocupación.<br>- [ ] Gráfico barras empleabilidad.<br>- [ ] Mapa de calor. |
| HU-12 | Gestión Servicios Infraestructura | Como Operador de Parque, quiero monitorear la disponibilidad de agua y servicios para garantizar operatividad. | R03 | historia-de-usuario | Baja | catastro | - [ ] Integración sensores agua cruda.<br>- [ ] Mapa de cortes de luz/gas. |
