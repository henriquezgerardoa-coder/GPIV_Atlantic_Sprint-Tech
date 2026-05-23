-- Corrige el constraint radicacion_historial_estado_check para incluir todos los
-- estados válidos del enum EstadoRadicacion, incluyendo RADICADA.

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

