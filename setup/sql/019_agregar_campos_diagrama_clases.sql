-- Migración 019: Alinear esquema con el diagrama de clases
-- NOTA: Con ddl-auto=update, las nuevas columnas y tablas se crean automáticamente al iniciar la app.
-- Este script documenta los cambios y puede ejecutarse manualmente si se usa ddl-auto=validate/none.

-- Nuevas columnas en radicaciones (datos del proyecto productivo propuesto)
ALTER TABLE radicaciones ADD COLUMN IF NOT EXISTS proyecto_productivo VARCHAR(120);
ALTER TABLE radicaciones ADD COLUMN IF NOT EXISTS rentabilidad_estimada DOUBLE PRECISION;
ALTER TABLE radicaciones ADD COLUMN IF NOT EXISTS empleados_previstos INTEGER;
ALTER TABLE radicaciones ADD COLUMN IF NOT EXISTS plan_ambiental TEXT;

-- Nueva columna rubro_id en empresas
ALTER TABLE lotes ADD COLUMN IF NOT EXISTS sector_id BIGINT REFERENCES sectores(id);

-- Las siguientes tablas son creadas por Hibernate al levantar la app (entidades nuevas):
-- sectores, rubros, proyectos_productivos, hitos_obra, documentos_pdf,
-- servicios, audit_log, censos, vehiculos, personal_registrado
