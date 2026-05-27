-- Renombrar columna en censos (cantidad_personas_no_registrado → cantidad_personal_no_registrado)
ALTER TABLE censos RENAME COLUMN cantidad_personas_no_registrado TO cantidad_personal_no_registrado;

-- Agregar columna tipo a vehiculos
ALTER TABLE vehiculos ADD COLUMN tipo VARCHAR(80) NOT NULL DEFAULT 'No especificado';