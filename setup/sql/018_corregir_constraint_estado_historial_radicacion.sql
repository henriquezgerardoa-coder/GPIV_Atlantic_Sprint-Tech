-- El constraint radicacion_historial_estado_check no incluía RADICADA como valor válido.
-- Se elimina y se recrea incluyendo todos los estados del enum EstadoRadicacion.

ALTER TABLE radicacion_historial
    DROP CONSTRAINT IF EXISTS radicacion_historial_estado_check;

ALTER TABLE radicacion_historial
    DROP CONSTRAINT IF EXISTS radicacion_historial_estado_anterior_check;

ALTER TABLE radicacion_historial
    ADD CONSTRAINT radicacion_historial_estado_check
        CHECK (estado IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RADICADA',
            'RECHAZADA',
            'REQUIERE_INFORMACION_ADICIONAL',
            'CANCELADA'
        ));

ALTER TABLE radicacion_historial
    ADD CONSTRAINT radicacion_historial_estado_anterior_check
        CHECK (estado_anterior IS NULL OR estado_anterior IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RADICADA',
            'RECHAZADA',
            'REQUIERE_INFORMACION_ADICIONAL',
            'CANCELADA'
        ));
