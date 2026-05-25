-- V13: Índices de rendimiento en columnas de alta frecuencia de consulta

-- radicaciones: columnas más usadas en WHERE, ORDER BY y JOINs
CREATE INDEX IF NOT EXISTS idx_radicaciones_empresa_id
    ON radicaciones(empresa_id);

CREATE INDEX IF NOT EXISTS idx_radicaciones_estado
    ON radicaciones(estado);

-- Índice compuesto para existsByEmpresaIdAndEstado (consulta frecuente en habilitaciones y validaciones)
CREATE INDEX IF NOT EXISTS idx_radicaciones_empresa_estado
    ON radicaciones(empresa_id, estado);

-- Índice compuesto para findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc
CREATE INDEX IF NOT EXISTS idx_radicaciones_empresa_fecha
    ON radicaciones(empresa_id, fecha_ultima_actualizacion DESC);

-- lotes: FK usada en findAllByEmpresaIdConEmpresa y existsByEmpresa_Id
CREATE INDEX IF NOT EXISTS idx_lotes_empresa_id
    ON lotes(empresa_id);

-- usuarios: FK usada en findByEmpresa_IdOrderByIdAsc
CREATE INDEX IF NOT EXISTS idx_usuarios_empresa_id
    ON usuarios(empresa_id);

-- radicacion_historial: FK usada en JOINs al cargar historial de radicación
CREATE INDEX IF NOT EXISTS idx_radicacion_historial_radicacion_id
    ON radicacion_historial(radicacion_id);

-- radicacion_documentos: FK usada en consultas de documentos por radicación
CREATE INDEX IF NOT EXISTS idx_radicacion_documentos_radicacion_id
    ON radicacion_documentos(radicacion_id);