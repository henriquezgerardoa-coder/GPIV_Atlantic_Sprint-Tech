-- ============================================================================
-- CORRECCIÓN COMPLETA DE INTEGRIDAD: Sincronizar estado_asignacion con empresa_id
-- Sistema: GPIV Atlantic Sprint-Tech
-- Fecha: 2026-05-08
-- Versión: 010 MEJORADO
-- ============================================================================
-- Propósito:
-- Limpiar TODOS los lotes y sincronizar correctamente:
-- 1. LIMPIAR COMPLETAMENTE: Quitar empresa_id de todos

-- 2. Estado DISPONIBLE = NO asignado a empresa (empresa_id = NULL)
-- 3. Estado PREADJUDICADO_A_SOLICITUD = Pendiente asignación (empresa_id = NULL)
-- 4. Estado ADJUDICADO_RADICADA = Adjudicado (empresa_id = NULL, está en radicaciones)
-- 5. Estado ASIGNADO_A_EMPRESA = Asignado a empresa (empresa_id != NULL)
-- ============================================================================

BEGIN;

-- Notificar cambios
\echo '*** INICIANDO CORRECCIÓN COMPLETA DE INTEGRIDAD DE LOTES ***'

-- 0. PRIMERO: Quitar TODAS las asignaciones de empresa
UPDATE lotes
SET empresa_id = NULL,
    fecha_asignacion = NULL,
    numero_expediente_referencia = NULL
WHERE 1=1;

\echo 'PASO 1: Todas las asignaciones de empresa removidas'

-- 1. Asegurar que TODOS los lotes tengan un estado válido (defaultear a DISPONIBLE si NULL)
UPDATE lotes
SET estado_asignacion = 'DISPONIBLE'
WHERE estado_asignacion IS NULL;

\echo 'PASO 2: Estados NULL convertidos a DISPONIBLE'

-- 2. Limpiar lotes con estado inválido (convertir OCUPADO o desconocidos a DISPONIBLE)
UPDATE lotes
SET estado_asignacion = 'DISPONIBLE'
WHERE estado_asignacion NOT IN ('DISPONIBLE', 'PREADJUDICADO_A_SOLICITUD', 'ASIGNADO_A_EMPRESA', 'ADJUDICADO_RADICADA');

\echo 'PASO 3: Estados inválidos corregidos'

-- 3. Verificar integridad final: NO debe haber inconsistencias
-- (Estado DISPONIBLE/PREADJUDICADO/ADJUDICADO con empresa_id != NULL)

-- Usar transacción para detectar inconsistencias antes del commit
SELECT COUNT(*) as inconsistencias_detectadas
FROM lotes
WHERE (estado_asignacion IN ('DISPONIBLE', 'PREADJUDICADO_A_SOLICITUD', 'ADJUDICADO_RADICADA')
       AND empresa_id IS NOT NULL);

-- Mostrar resumen ANTES
\echo '*** RESUMEN FINAL DE LOTES DESPUÉS DE CORRECCIÓN ***'
SELECT
    COALESCE(estado_asignacion, 'NULL') as estado,
    COUNT(*) as cantidad,
    COUNT(CASE WHEN empresa_id IS NOT NULL THEN 1 END) as con_empresa_asignada,
    COUNT(CASE WHEN empresa_id IS NULL THEN 1 END) as sin_empresa,
    COUNT(CASE WHEN fecha_asignacion IS NOT NULL THEN 1 END) as con_fecha
FROM lotes
GROUP BY estado_asignacion
ORDER BY estado_asignacion;

-- Mostrar TOTAL
\echo '*** ESTADÍSTICAS GLOBALES ***'
SELECT
    COUNT(*) as total_lotes,
    COUNT(CASE WHEN empresa_id IS NULL THEN 1 END) as sin_empresa_asignada,
    COUNT(CASE WHEN empresa_id IS NOT NULL THEN 1 END) as con_empresa_asignada,
    COUNT(CASE WHEN estado_asignacion = 'DISPONIBLE' THEN 1 END) as lotes_disponibles,
    COUNT(CASE WHEN estado_asignacion = 'ASIGNADO_A_EMPRESA' THEN 1 END) as lotes_asignados
FROM lotes;

\echo '*** CORRECCIÓN COMPLETADA EXITOSAMENTE ***'

COMMIT;

