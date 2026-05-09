-- Migración 015: Insertar 123 lotes del Parque Industrial ENREPAVI
--   · 63 lotes en Parque Viejo de 5.000 m² (PV-001 … PV-063)
--   · 20 lotes en Parque Nuevo de 1.250 m² (PN-A-001 … PN-A-020)
--   · 20 lotes en Parque Nuevo de 2.500 m² (PN-B-001 … PN-B-020)
--   · 20 lotes en Parque Nuevo de 5.000 m² (PN-C-001 … PN-C-020)
-- Se usa WHERE NOT EXISTS para poder re-ejecutar el script sin duplicar registros.

BEGIN;

-- Parque Viejo — 63 lotes de 5.000 m²
INSERT INTO lotes (codigo, superficie_m2, estado_asignacion, zona, ocupado)
SELECT
    'PV-' || LPAD(n::text, 3, '0'),
    5000.0,
    'DISPONIBLE',
    'PARQUE_VIEJO',
    false
FROM generate_series(1, 63) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PV-' || LPAD(n::text, 3, '0')
      AND empresa_id IS NULL
);

-- Parque Nuevo — 20 lotes de 1.250 m²
INSERT INTO lotes (codigo, superficie_m2, estado_asignacion, zona, ocupado)
SELECT
    'PN-A-' || LPAD(n::text, 3, '0'),
    1250.0,
    'DISPONIBLE',
    'PARQUE_NUEVO',
    false
FROM generate_series(1, 20) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PN-A-' || LPAD(n::text, 3, '0')
      AND empresa_id IS NULL
);

-- Parque Nuevo — 20 lotes de 2.500 m²
INSERT INTO lotes (codigo, superficie_m2, estado_asignacion, zona, ocupado)
SELECT
    'PN-B-' || LPAD(n::text, 3, '0'),
    2500.0,
    'DISPONIBLE',
    'PARQUE_NUEVO',
    false
FROM generate_series(1, 20) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PN-B-' || LPAD(n::text, 3, '0')
      AND empresa_id IS NULL
);

-- Parque Nuevo — 20 lotes de 5.000 m²
INSERT INTO lotes (codigo, superficie_m2, estado_asignacion, zona, ocupado)
SELECT
    'PN-C-' || LPAD(n::text, 3, '0'),
    5000.0,
    'DISPONIBLE',
    'PARQUE_NUEVO',
    false
FROM generate_series(1, 20) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PN-C-' || LPAD(n::text, 3, '0')
      AND empresa_id IS NULL
);

COMMIT;