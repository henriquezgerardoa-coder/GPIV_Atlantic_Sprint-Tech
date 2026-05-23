-- Extiende el bloque de servicios post-radicacion con un JSON flexible para
-- consumos (agua cruda, luz, gas, internet) y futuros servicios.

ALTER TABLE empresas
    ADD COLUMN IF NOT EXISTS servicios_post_radicacion_json VARCHAR(20000);

