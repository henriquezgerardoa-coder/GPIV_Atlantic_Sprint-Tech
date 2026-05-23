-- V6: Soporte de consultas públicas en mensajería

ALTER TABLE mensajeria_conversaciones
    ADD COLUMN IF NOT EXISTS tipo_origen VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contacto_nombre_empresa VARCHAR(160),
    ADD COLUMN IF NOT EXISTS contacto_correo_electronico VARCHAR(160),
    ADD COLUMN IF NOT EXISTS contacto_telefono VARCHAR(40);

UPDATE mensajeria_conversaciones
SET tipo_origen = 'EMPRESA'
WHERE tipo_origen IS NULL;

ALTER TABLE mensajeria_conversaciones
    ALTER COLUMN tipo_origen SET NOT NULL;

ALTER TABLE mensajeria_conversaciones
    ALTER COLUMN empresa_id DROP NOT NULL;

ALTER TABLE mensajeria_mensajes
    ADD COLUMN IF NOT EXISTS emisor_externo_nombre VARCHAR(160);

ALTER TABLE mensajeria_mensajes
    ALTER COLUMN usuario_emisor_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mensajeria_conversaciones_origen_fecha
    ON mensajeria_conversaciones (tipo_origen, fecha_ultima_actualizacion DESC);

