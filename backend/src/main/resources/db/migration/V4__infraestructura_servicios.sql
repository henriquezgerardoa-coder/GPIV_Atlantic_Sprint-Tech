-- HU-12: Monitoreo de infraestructura / estación de bombeo

ALTER TABLE servicios ADD COLUMN IF NOT EXISTS fecha_ultima_actualizacion TIMESTAMP DEFAULT NOW();

CREATE TABLE IF NOT EXISTS servicio_eventos (
    id           BIGSERIAL PRIMARY KEY,
    servicio_id  BIGINT       NOT NULL REFERENCES servicios(id),
    estado       VARCHAR(40)  NOT NULL,
    comentario   VARCHAR(500),
    usuario      VARCHAR(80)  NOT NULL,
    fecha_evento TIMESTAMP    NOT NULL
);

-- Datos iniciales solo si la tabla está vacía
INSERT INTO servicios (nombre, descripcion_tecnica, estado_actual, fecha_ultima_actualizacion)
SELECT nombre, descripcion_tecnica, estado_actual, NOW()
FROM (VALUES
    ('Estación de bombeo — Agua cruda',    'Bombeo y distribución de agua cruda desde toma del río Negro'),
    ('Red eléctrica',                       'Suministro eléctrico general del parque industrial'),
    ('Red de gas natural',                  'Red de distribución de gas natural a los lotes'),
    ('Acceso a internet / fibra óptica',   'Conectividad de red para las empresas radicadas'),
    ('Sistema de tratamiento de efluentes','Planta de tratamiento de aguas servidas del parque')
) AS s(nombre, descripcion_tecnica)
CROSS JOIN (VALUES ('OPERATIVO')) AS e(estado_actual)
WHERE NOT EXISTS (SELECT 1 FROM servicios LIMIT 1);
