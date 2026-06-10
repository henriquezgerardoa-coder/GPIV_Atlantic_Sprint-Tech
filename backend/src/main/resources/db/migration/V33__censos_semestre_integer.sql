-- Corrige el tipo de la columna semestre en censos de SMALLINT a INTEGER
-- para que coincida con el mapeo JPA (int Java -> INTEGER)
ALTER TABLE censos ALTER COLUMN semestre TYPE INTEGER;
