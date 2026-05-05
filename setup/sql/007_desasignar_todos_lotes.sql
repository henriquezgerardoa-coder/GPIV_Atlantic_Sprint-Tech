BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'lotes'
    ) THEN
        UPDATE lotes
        SET empresa_id = NULL,
            fecha_asignacion = NULL,
            estado_asignacion = NULL,
            numero_expediente_referencia = NULL
        WHERE empresa_id IS NOT NULL
           OR fecha_asignacion IS NOT NULL
           OR estado_asignacion IS NOT NULL
           OR numero_expediente_referencia IS NOT NULL;
    END IF;
END $$;

COMMIT;

