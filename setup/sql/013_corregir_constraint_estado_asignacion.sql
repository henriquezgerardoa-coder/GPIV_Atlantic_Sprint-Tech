BEGIN;

ALTER TABLE public.lotes
    DROP CONSTRAINT IF EXISTS lotes_estado_asignacion_check;

ALTER TABLE public.lotes
    ADD CONSTRAINT lotes_estado_asignacion_check
        CHECK (estado_asignacion IN (
            'DISPONIBLE',
            'PREADJUDICADO_A_SOLICITUD',
            'ASIGNADO_A_EMPRESA',
            'ADJUDICADO_RADICADA'
        ));

INSERT INTO public.schema_version (version, descripcion, tipo_cambio, fecha_aplicacion, estado)
VALUES ('013', 'Reemplazar check constraint obsoleto de estado_asignacion con valores del enum actual', 'ALTER', NOW(), 'APLICADA')
ON CONFLICT (version) DO NOTHING;

COMMIT;