-- Registra la fecha en que la solicitud fue aprobada (estado APROBADA)
-- Permite calcular la fecha estimada de radicación = fecha_aprobacion + tiempo_radicacion_meses

ALTER TABLE radicaciones
    ADD COLUMN IF NOT EXISTS fecha_aprobacion DATE;