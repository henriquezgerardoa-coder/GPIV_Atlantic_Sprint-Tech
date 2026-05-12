-- Migración 012: Agrega campo zona a la tabla lotes
-- Permite distinguir entre Parque Viejo y Parque Nuevo
BEGIN;

ALTER TABLE public.lotes
    ADD COLUMN IF NOT EXISTS zona VARCHAR(20);

INSERT INTO public.schema_version (version, descripcion, tipo_cambio, fecha_aplicacion, estado)
VALUES ('012', 'Agregar columna zona a lotes (PARQUE_VIEJO / PARQUE_NUEVO)', 'ALTER', NOW(), 'APLICADA')
ON CONFLICT (version) DO NOTHING;

COMMIT;