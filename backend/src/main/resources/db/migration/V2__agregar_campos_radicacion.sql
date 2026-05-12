-- Agrega campos del diagrama de clases a la tabla radicaciones
-- Campos incorporados a RadicacionSolicitud: rentabilidadEstimada, empleadosPrevistos, planAmbiental

ALTER TABLE radicaciones
    ADD COLUMN IF NOT EXISTS rentabilidad_estimada DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS empleados_previstos   INTEGER,
    ADD COLUMN IF NOT EXISTS plan_ambiental        VARCHAR(2000);
