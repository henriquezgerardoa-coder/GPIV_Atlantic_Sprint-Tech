BEGIN;

ALTER TABLE empresas ADD COLUMN IF NOT EXISTS razon_social VARCHAR(160);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS nit VARCHAR(30);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS direccion VARCHAR(240);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS actividad_economica VARCHAR(180);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS telefono VARCHAR(40);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS status VARCHAR(30);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS cantidad_empleados INTEGER;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS vehiculos_asignados_json VARCHAR(12000);

UPDATE empresas SET razon_social = nombre WHERE razon_social IS NULL OR btrim(razon_social) = '';
UPDATE empresas SET nit = cuit WHERE nit IS NULL OR btrim(nit) = '';
UPDATE empresas SET direccion = '' WHERE direccion IS NULL;
UPDATE empresas SET actividad_economica = 'No definida' WHERE actividad_economica IS NULL OR btrim(actividad_economica) = '';
UPDATE empresas SET correo_electronico = '' WHERE correo_electronico IS NULL;
UPDATE empresas SET fecha_registro = NOW() WHERE fecha_registro IS NULL;
UPDATE empresas SET status = 'ACTIVA' WHERE status IS NULL OR btrim(status) = '';
UPDATE empresas SET cantidad_empleados = 0 WHERE cantidad_empleados IS NULL;

ALTER TABLE empresas ALTER COLUMN razon_social SET NOT NULL;
ALTER TABLE empresas ALTER COLUMN nit SET NOT NULL;
ALTER TABLE empresas ALTER COLUMN direccion SET NOT NULL;
ALTER TABLE empresas ALTER COLUMN actividad_economica SET NOT NULL;
ALTER TABLE empresas ALTER COLUMN fecha_registro SET NOT NULL;
ALTER TABLE empresas ALTER COLUMN status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_empresa_nit'
    ) THEN
        ALTER TABLE empresas ADD CONSTRAINT uk_empresa_nit UNIQUE (nit);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_empresa_cuit'
    ) THEN
        ALTER TABLE empresas ADD CONSTRAINT uk_empresa_cuit UNIQUE (cuit);
    END IF;
END
$$;

COMMIT;

