BEGIN;

CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE lotes
    ADD COLUMN IF NOT EXISTS poligono geometry(Polygon,4326);

CREATE INDEX IF NOT EXISTS idx_lotes_poligono_gist
    ON lotes
    USING GIST (poligono);

COMMIT;

