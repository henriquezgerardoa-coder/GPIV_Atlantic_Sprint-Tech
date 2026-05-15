-- 020: Sectores y lotes del parque industrial ENREPAVI
-- Parque Viejo: 63 lotes de 5000 m²
-- Parque Nuevo: 20 de 1250 m², 20 de 2500 m², 20 de 5000 m²

INSERT INTO sectores (nombre) VALUES ('Parque Viejo') ON CONFLICT (nombre) DO NOTHING;
INSERT INTO sectores (nombre) VALUES ('Parque Nuevo') ON CONFLICT (nombre) DO NOTHING;

-- Parque Viejo: PV-01 … PV-63 (5000 m²)
INSERT INTO lotes (codigo, superficie_m2, ocupado, sector_id)
SELECT
    'PV-' || LPAD(n::TEXT, 2, '0'),
    5000.0,
    false,
    (SELECT id FROM sectores WHERE nombre = 'Parque Viejo')
FROM generate_series(1, 63) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PV-' || LPAD(n::TEXT, 2, '0')
      AND empresa_id IS NULL
);

-- Parque Nuevo: PN-01 … PN-20 (1250 m²)
INSERT INTO lotes (codigo, superficie_m2, ocupado, sector_id)
SELECT
    'PN-' || LPAD(n::TEXT, 2, '0'),
    1250.0,
    false,
    (SELECT id FROM sectores WHERE nombre = 'Parque Nuevo')
FROM generate_series(1, 20) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PN-' || LPAD(n::TEXT, 2, '0')
      AND empresa_id IS NULL
);

-- Parque Nuevo: PN-21 … PN-40 (2500 m²)
INSERT INTO lotes (codigo, superficie_m2, ocupado, sector_id)
SELECT
    'PN-' || LPAD(n::TEXT, 2, '0'),
    2500.0,
    false,
    (SELECT id FROM sectores WHERE nombre = 'Parque Nuevo')
FROM generate_series(21, 40) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PN-' || LPAD(n::TEXT, 2, '0')
      AND empresa_id IS NULL
);

-- Parque Nuevo: PN-41 … PN-60 (5000 m²)
INSERT INTO lotes (codigo, superficie_m2, ocupado, sector_id)
SELECT
    'PN-' || LPAD(n::TEXT, 2, '0'),
    5000.0,
    false,
    (SELECT id FROM sectores WHERE nombre = 'Parque Nuevo')
FROM generate_series(41, 60) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM lotes
    WHERE codigo = 'PN-' || LPAD(n::TEXT, 2, '0')
      AND empresa_id IS NULL
);
