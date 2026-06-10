-- V30: Agrega columna color_personalizado a lotes para asignación visual desde el mapa.
ALTER TABLE lotes ADD COLUMN IF NOT EXISTS color_personalizado VARCHAR(20);