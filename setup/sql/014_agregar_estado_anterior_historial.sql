ALTER TABLE radicacion_historial
    ADD COLUMN IF NOT EXISTS estado_anterior VARCHAR(40);