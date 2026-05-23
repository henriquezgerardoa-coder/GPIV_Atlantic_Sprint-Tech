ALTER TABLE public.empresas
    DROP CONSTRAINT IF EXISTS uk_empresa_nit;

ALTER TABLE public.empresas
    DROP COLUMN IF EXISTS nit;

