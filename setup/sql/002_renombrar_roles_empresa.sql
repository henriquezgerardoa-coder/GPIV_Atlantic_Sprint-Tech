DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'usuarios_roles'
    ) THEN
        UPDATE public.usuarios_roles
        SET rol = 'EMPRESA'
        WHERE UPPER(rol) IN (
            'EMPRESA_CONSULTORA',
            'CLIENTE'
        );
    END IF;
END $$;
