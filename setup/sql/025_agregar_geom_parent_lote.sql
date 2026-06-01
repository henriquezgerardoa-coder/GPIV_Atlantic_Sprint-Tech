-- 025_agregar_geom_parent_lote.sql
-- Añade soporte espacial y campos para subdivisiones de lotes
-- Crea la extensión postgis si no existe, agrega columnas geom, parent_lote_id, external_id y properties

-- Habilitar PostGIS (no tiene efecto si ya está instalada)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Agregar columnas necesarias para relacionar geometría y jerarquía de lotes
ALTER TABLE lotes
  ADD COLUMN IF NOT EXISTS geom geometry(Polygon,4326),
  ADD COLUMN IF NOT EXISTS parent_lote_id bigint,
  ADD COLUMN IF NOT EXISTS external_id varchar(100),
  ADD COLUMN IF NOT EXISTS properties jsonb;

-- Añadir constraint FK self-reference para parent_lote_id
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'lotes' AND tc.constraint_type = 'FOREIGN KEY' AND kcu.column_name = 'parent_lote_id'
  ) THEN
    ALTER TABLE lotes
      ADD CONSTRAINT fk_lotes_parent FOREIGN KEY (parent_lote_id) REFERENCES lotes(id);
  END IF;
END$$;

-- Crear índice espacial para consultas por geometría
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_lotes_geom') THEN
    EXECUTE 'CREATE INDEX idx_lotes_geom ON lotes USING GIST (geom)';
  END IF;
END$$;

-- Índice único para external_id cuando no es null (facilita sincronización con GeoJSON)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'ux_lotes_external_id') THEN
    EXECUTE 'CREATE UNIQUE INDEX ux_lotes_external_id ON lotes (external_id) WHERE external_id IS NOT NULL';
  END IF;
END$$;

-- Opcional: si quieres actualizar superficies desde geometría existente (si geom está llena)
-- UPDATE lotes SET superficie_m2 = ST_Area(ST_Transform(geom,3857)) WHERE geom IS NOT NULL AND (superficie_m2 IS NULL OR superficie_m2 = 0);

