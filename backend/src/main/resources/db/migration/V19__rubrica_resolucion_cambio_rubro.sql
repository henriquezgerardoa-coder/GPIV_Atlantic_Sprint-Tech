ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS rubrica_puntaje_impacto_operativo SMALLINT;

ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS rubrica_puntaje_compatibilidad_parque SMALLINT;

ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS rubrica_puntaje_viabilidad_tecnica SMALLINT;

ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS rubrica_puntaje_cumplimiento_normativo SMALLINT;

ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS rubrica_observaciones VARCHAR(1000);

ALTER TABLE solicitudes_cambio_rubro
    DROP CONSTRAINT IF EXISTS chk_solicitudes_cambio_rubro_puntaje_impacto_operativo;

ALTER TABLE solicitudes_cambio_rubro
    ADD CONSTRAINT chk_solicitudes_cambio_rubro_puntaje_impacto_operativo
        CHECK (rubrica_puntaje_impacto_operativo IS NULL
            OR rubrica_puntaje_impacto_operativo BETWEEN 1 AND 5);

ALTER TABLE solicitudes_cambio_rubro
    DROP CONSTRAINT IF EXISTS chk_solicitudes_cambio_rubro_puntaje_compatibilidad_parque;

ALTER TABLE solicitudes_cambio_rubro
    ADD CONSTRAINT chk_solicitudes_cambio_rubro_puntaje_compatibilidad_parque
        CHECK (rubrica_puntaje_compatibilidad_parque IS NULL
            OR rubrica_puntaje_compatibilidad_parque BETWEEN 1 AND 5);

ALTER TABLE solicitudes_cambio_rubro
    DROP CONSTRAINT IF EXISTS chk_solicitudes_cambio_rubro_puntaje_viabilidad_tecnica;

ALTER TABLE solicitudes_cambio_rubro
    ADD CONSTRAINT chk_solicitudes_cambio_rubro_puntaje_viabilidad_tecnica
        CHECK (rubrica_puntaje_viabilidad_tecnica IS NULL
            OR rubrica_puntaje_viabilidad_tecnica BETWEEN 1 AND 5);

ALTER TABLE solicitudes_cambio_rubro
    DROP CONSTRAINT IF EXISTS chk_solicitudes_cambio_rubro_puntaje_cumplimiento_normativo;

ALTER TABLE solicitudes_cambio_rubro
    ADD CONSTRAINT chk_solicitudes_cambio_rubro_puntaje_cumplimiento_normativo
        CHECK (rubrica_puntaje_cumplimiento_normativo IS NULL
            OR rubrica_puntaje_cumplimiento_normativo BETWEEN 1 AND 5);

