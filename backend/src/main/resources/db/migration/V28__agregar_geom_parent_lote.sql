-- V28__agregar_geom_parent_lote.sql
-- Flyway migration que añade soporte espacial y campos para subdivisiones de lotes

CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE lotes
  ADD COLUMN IF NOT EXISTS geom geometry(Polygon,4326),
  ADD COLUMN IF NOT EXISTS parent_lote_id bigint,
  ADD COLUMN IF NOT EXISTS external_id varchar(100),
  ADD COLUMN IF NOT EXISTS properties jsonb;

-- Añadir constraint FK (sin IF NOT EXISTS porque no está soportado en todas las versiones de Postgres)
ALTER TABLE IF EXISTS lotes
  ADD CONSTRAINT fk_lotes_parent FOREIGN KEY (parent_lote_id) REFERENCES lotes(id);

CREATE INDEX IF NOT EXISTS idx_lotes_geom ON lotes USING GIST (geom);

CREATE UNIQUE INDEX IF NOT EXISTS ux_lotes_external_id ON lotes (external_id) WHERE external_id IS NOT NULL;

-- Nota: ST_Area en 4326 no devuelve m2. Si se desea poblar superficie_m2 desde geom, usar ST_Transform a una proyección métrica.
-- UPDATE lotes SET superficie_m2 = ST_Area(ST_Transform(geom,3857)) WHERE geom IS NOT NULL AND (superficie_m2 IS NULL OR superficie_m2 = 0);

