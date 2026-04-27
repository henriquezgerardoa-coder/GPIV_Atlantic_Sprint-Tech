DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'usuarios'
          AND column_name = 'email_verificado'
    ) THEN
        UPDATE public.usuarios
        SET email_verificado = false
        WHERE email_verificado IS NULL;

        ALTER TABLE public.usuarios
            ALTER COLUMN email_verificado SET DEFAULT false;

        ALTER TABLE public.usuarios
            ALTER COLUMN email_verificado SET NOT NULL;
    END IF;
END $$;

