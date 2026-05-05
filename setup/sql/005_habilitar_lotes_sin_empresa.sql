BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'lotes'
          AND column_name = 'empresa_id'
    ) THEN
        ALTER TABLE lotes
            ALTER COLUMN empresa_id DROP NOT NULL,
            ALTER COLUMN empresa_id DROP DEFAULT;
    END IF;
END $$;

COMMIT;

