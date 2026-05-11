-- ============================================================================
-- MIGRACIÓN: Crear tabla "medidas_solicitud"
-- Sistema: GPIV Atlantic Sprint-Tech
-- Fecha: 2026-05-07
-- Versión: 009
-- ============================================================================
-- Propósito:
-- Permite definir requisitos técnicos (medidas) para cada solicitud,
-- permitiendo que Admin/Directivo validen si un lote asignado cumple
-- con los requisitos especificados.
--
-- Ejemplo de medidas:
-- - Superficie mínima 500 m²
-- - Acceso desde ruta norte
-- - Conexión a agua potable
-- - Distancia máxima 2 km del centro urbano
-- ============================================================================

BEGIN;

-- Crear ENUM para tipo de medida (si se usa PostgreSQL)
-- Nota: Si no funciona, comentar y usar CHECK constraint en su lugar
DROP TYPE IF EXISTS tipo_medida CASCADE;
CREATE TYPE tipo_medida AS ENUM (
    'SUPERFICIE',
    'ACCESO',
    'SERVICIOS',
    'DISTANCIA',
    'OTRO'
);

-- Crear tabla medidas_solicitud
CREATE TABLE IF NOT EXISTS medidas_solicitud (
    id BIGSERIAL PRIMARY KEY,

    -- Descripción legible de la medida
    descripcion TEXT NOT NULL,

    -- Tipo de medida
    tipo tipo_medida NOT NULL,

    -- Valor específico (ej: "500" para 500m², "NORTE" para acceso norte)
    valor VARCHAR(100) NOT NULL,

    -- Referencia a la solicitud que requiere esta medida
    solicitud_id BIGINT NOT NULL,

    -- Timestamps
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Restricción de clave foránea
    CONSTRAINT fk_medida_solicitud
        FOREIGN KEY (solicitud_id)
        REFERENCES solicitudes(id)
        ON DELETE CASCADE
);

-- Crear índices para optimizar consultas
CREATE INDEX idx_medida_solicitud_id ON medidas_solicitud(solicitud_id);
CREATE INDEX idx_medida_tipo ON medidas_solicitud(tipo);

-- Agregar comentarios a la tabla y columnas
COMMENT ON TABLE medidas_solicitud IS
    'Medidas o requisitos técnicos de una solicitud de asignación de lotes';

COMMENT ON COLUMN medidas_solicitud.id IS
    'Identificador único de la medida';

COMMENT ON COLUMN medidas_solicitud.descripcion IS
    'Descripción legible de la medida (ej: "Superficie mínima 500 m²")';

COMMENT ON COLUMN medidas_solicitud.tipo IS
    'Tipo de medida: SUPERFICIE, ACCESO, SERVICIOS, DISTANCIA, OTRO';

COMMENT ON COLUMN medidas_solicitud.valor IS
    'Valor específico de la medida (ej: "500" para m², "NORTE" para acceso)';

COMMENT ON COLUMN medidas_solicitud.solicitud_id IS
    'ID de la solicitud a la que pertenece esta medida';

-- Agregar la columna a solicitudes si es necesario (para auditoría)
ALTER TABLE solicitudes
ADD COLUMN IF NOT EXISTS cantidad_medidas INT DEFAULT 0;

-- Crear vista para facilitar consultas comunes
CREATE OR REPLACE VIEW v_solicitudes_con_medidas AS
SELECT
    s.id,
    s.id_tramite,
    s.cuit_postulante,
    s.rubro,
    s.estado,
    s.fecha_creacion,
    COUNT(m.id) as cantidad_medidas,
    STRING_AGG(m.descripcion, ' | ') as medidas_descripciones,
    STRING_AGG(DISTINCT m.tipo::TEXT, ', ') as tipos_medidas
FROM solicitudes s
LEFT JOIN medidas_solicitud m ON s.id = m.solicitud_id
GROUP BY s.id, s.id_tramite, s.cuit_postulante, s.rubro, s.estado, s.fecha_creacion
ORDER BY s.fecha_creacion DESC;

COMMENT ON VIEW v_solicitudes_con_medidas IS
    'Vista para consultas rápidas de solicitudes con información de su medidas adjuntas';

-- Insertar datos de ejemplo (descomentar si es necesario)
/*
INSERT INTO medidas_solicitud (descripcion, tipo, valor, solicitud_id)
VALUES
    ('Superficie mínima 500 m²', 'SUPERFICIE', '500', 1),
    ('Acceso desde ruta norte', 'ACCESO', 'NORTE', 1),
    ('Conexión a agua potable', 'SERVICIOS', 'AGUA_POTABLE', 1),
    ('Distancia máxima 2 km del centro', 'DISTANCIA', '2', 1);
*/

-- Log de la migración
INSERT INTO schema_version (
    version,
    descripcion,
    tipo_cambio,
    fecha_aplicacion,
    estado
) VALUES (
    '009',
    'Crear tabla medidas_solicitud para requisitos técnicos de solicitudes',
    'CREATE_TABLE',
    CURRENT_TIMESTAMP,
    'COMPLETADO'
) ON CONFLICT DO NOTHING;

COMMIT;

-- Validar la creación
SELECT 'Tabla medidas_solicitud creada exitosamente' AS resultado;
SELECT COUNT(*) as columnas FROM information_schema.columns WHERE table_name = 'medidas_solicitud';


