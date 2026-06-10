-- V31: Ciclo de vida del lote (7 etapas)
-- Reemplaza el campo estado_asignacion (3 valores) por etapa (7 valores)
-- y crea la tabla de historial de transiciones.

-- 1) Columnas nuevas en lotes
ALTER TABLE lotes
    ADD COLUMN IF NOT EXISTS etapa VARCHAR(40),
    ADD COLUMN IF NOT EXISTS fecha_inicio_etapa_actual DATE,
    ADD COLUMN IF NOT EXISTS fecha_limite_etapa_actual DATE;

-- 2) Backfill desde estado_asignacion
UPDATE lotes SET etapa = 'DISPONIBLE'
    WHERE empresa_id IS NULL OR ocupado = false;

UPDATE lotes
    SET etapa = 'PROYECTO_EN_EVALUACION',
        fecha_inicio_etapa_actual = COALESCE(fecha_asignacion, CURRENT_DATE),
        fecha_limite_etapa_actual = COALESCE(fecha_asignacion, CURRENT_DATE) + INTERVAL '90 days'
    WHERE estado_asignacion = 'PREADJUDICADO' AND empresa_id IS NOT NULL AND ocupado = true;

UPDATE lotes
    SET etapa = 'OPERATIVO',
        fecha_inicio_etapa_actual = COALESCE(fecha_asignacion, CURRENT_DATE)
    WHERE estado_asignacion = 'ADJUDICADO' AND empresa_id IS NOT NULL AND ocupado = true;

UPDATE lotes
    SET etapa = 'REVERTIDO',
        fecha_inicio_etapa_actual = COALESCE(fecha_asignacion, CURRENT_DATE)
    WHERE estado_asignacion = 'DESADJUDICADO';

-- Lotes que aún sean NULL (borde de datos inconsistentes)
UPDATE lotes SET etapa = 'DISPONIBLE' WHERE etapa IS NULL;

-- 3) Activar NOT NULL una vez relleno
ALTER TABLE lotes ALTER COLUMN etapa SET NOT NULL;

-- 4) Eliminar columna legacy
ALTER TABLE lotes DROP COLUMN IF EXISTS estado_asignacion;

-- 5) Tabla historial de etapas
CREATE TABLE IF NOT EXISTS lote_historial_etapa (
    id                   BIGSERIAL    PRIMARY KEY,
    lote_id              BIGINT       NOT NULL REFERENCES lotes(id) ON DELETE CASCADE,
    etapa_anterior       VARCHAR(40),
    etapa_nueva          VARCHAR(40)  NOT NULL,
    fecha_cambio         TIMESTAMP    NOT NULL,
    usuario_responsable  VARCHAR(120) NOT NULL,
    motivo               VARCHAR(2000),
    fecha_limite_plazo   DATE
);

-- 6) Índices
CREATE INDEX IF NOT EXISTS idx_lote_historial_lote ON lote_historial_etapa(lote_id);
CREATE INDEX IF NOT EXISTS idx_lote_historial_fecha ON lote_historial_etapa(fecha_cambio DESC);
CREATE INDEX IF NOT EXISTS idx_lotes_etapa ON lotes(etapa);
CREATE INDEX IF NOT EXISTS idx_lotes_fecha_limite ON lotes(fecha_limite_etapa_actual)
    WHERE fecha_limite_etapa_actual IS NOT NULL;

-- 7) Seed historial inicial para lotes con etapa <> DISPONIBLE
INSERT INTO lote_historial_etapa
    (lote_id, etapa_anterior, etapa_nueva, fecha_cambio, usuario_responsable, motivo, fecha_limite_plazo)
SELECT
    id,
    NULL,
    etapa,
    COALESCE(fecha_inicio_etapa_actual::timestamp, NOW()),
    'sistema@migracion',
    'Migración inicial al ciclo de vida (V31)',
    fecha_limite_etapa_actual
FROM lotes
WHERE etapa <> 'DISPONIBLE';

-- 8) Lote de prueba para alertas de vencimiento (solo si existe LN014)
-- Simula un lote en ADJUDICADO_PRECARIO con plazo vencido hace 10 días.
UPDATE lotes
    SET etapa = 'ADJUDICADO_PRECARIO',
        fecha_inicio_etapa_actual = CURRENT_DATE - INTERVAL '190 days',
        fecha_limite_etapa_actual = CURRENT_DATE - INTERVAL '10 days'
    WHERE codigo = 'LN014' AND empresa_id IS NOT NULL;
