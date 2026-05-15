-- Agrega campos de tiempo estimado de obra y fecha de plazo a radicaciones
-- Se requieren cuando la solicitud pasa a estado APROBADA

ALTER TABLE radicaciones
    ADD COLUMN IF NOT EXISTS tiempo_estimado_obra VARCHAR(200),
    ADD COLUMN IF NOT EXISTS fecha_plazo_aprobacion DATE;
