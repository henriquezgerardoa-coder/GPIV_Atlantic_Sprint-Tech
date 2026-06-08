-- Migración 026: Agregar columnas geométricas y de relación a lotes
-- Esta migración prepara la tabla lotes para almacenar geometrías desde GeoJSON

-- 1. Agregar columna geom (requiere PostGIS habilitado)
-- Si PostGIS no está habilitado, ejecutar: CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE lotes
ADD COLUMN IF NOT EXISTS geom geometry(Polygon,4326),
ADD COLUMN IF NOT EXISTS external_id VARCHAR(100) UNIQUE,
ADD COLUMN IF NOT EXISTS properties jsonb,
ADD COLUMN IF NOT EXISTS parent_lote_id BIGINT REFERENCES lotes(id) ON DELETE SET NULL;

-- Crear índices para mejores búsquedas
CREATE INDEX IF NOT EXISTS idx_lotes_geom ON lotes USING GIST(geom);
CREATE INDEX IF NOT EXISTS idx_lotes_external_id ON lotes(external_id);
CREATE INDEX IF NOT EXISTS idx_lotes_parent_id ON lotes(parent_lote_id);

-- Crear índice JSONB para búsquedas en properties
CREATE INDEX IF NOT EXISTS idx_lotes_properties ON lotes USING gin(properties);

-- Comando informativo
-- SELECT COUNT(*) FROM lotes WHERE geom IS NULL; -- Para conocer cuántos lotes faltan geometría
-- SELECT ST_AsGeoJSON(geom) FROM lotes WHERE geom IS NOT NULL LIMIT 1; -- Para ver formato GeoJSON

