-- HU-09: registro de la última vez que se envió alerta para cada hito,
-- para evitar notificaciones duplicadas en el mismo día.
ALTER TABLE hitos_obra
    ADD COLUMN IF NOT EXISTS ultima_notificacion_enviada DATE;