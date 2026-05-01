DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'usuarios_roles'
    ) THEN
        UPDATE public.usuarios_roles
        SET rol = 'DIRECTIVO'
        WHERE UPPER(rol) = 'OPERADOR';

        IF EXISTS (
            SELECT 1
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE n.nspname = 'public'
              AND t.relname = 'usuarios_roles'
              AND c.conname = 'usuarios_roles_rol_check'
        ) THEN
            ALTER TABLE public.usuarios_roles DROP CONSTRAINT usuarios_roles_rol_check;
        END IF;

        ALTER TABLE public.usuarios_roles
            ADD CONSTRAINT usuarios_roles_rol_check
            CHECK (rol IN ('ADMINISTRADOR', 'DIRECTIVO', 'EMPRESA'));
    END IF;
END $$;

