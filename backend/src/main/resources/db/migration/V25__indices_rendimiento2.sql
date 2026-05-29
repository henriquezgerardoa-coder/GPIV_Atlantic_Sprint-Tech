-- V25: Índices de rendimiento en FK y columnas de alta frecuencia no cubiertas en V13

-- hitos_obra: FK usada en JOIN FETCH y batch-loading desde proyectos_productivos
CREATE INDEX IF NOT EXISTS idx_hitos_obra_proyecto_id
    ON hitos_obra(proyecto_id);

-- Índice parcial para consultas de hitos vencidos (findHitosVencidos, findHitosVencidosSinNotificar)
CREATE INDEX IF NOT EXISTS idx_hitos_obra_vencidos
    ON hitos_obra(fecha_vencimiento_real ASC)
    WHERE cumplido = false;

-- proyectos_productivos: FK usada en JOINs con radicaciones y responsable
CREATE INDEX IF NOT EXISTS idx_proyectos_solicitud_origen_id
    ON proyectos_productivos(solicitud_origen_id);

CREATE INDEX IF NOT EXISTS idx_proyectos_responsable_id
    ON proyectos_productivos(responsable_id);

-- usuarios_roles: FK usada en batch-loading de roles por usuario
CREATE INDEX IF NOT EXISTS idx_usuarios_roles_usuario_id
    ON usuarios_roles(usuario_id);

-- audit_log: columnas usadas en WHERE y ORDER BY
CREATE INDEX IF NOT EXISTS idx_audit_log_entidad_afectada
    ON audit_log(entidad_afectada);

CREATE INDEX IF NOT EXISTS idx_audit_log_usuario_responsable
    ON audit_log(usuario_responsable);

CREATE INDEX IF NOT EXISTS idx_audit_log_fecha_hora
    ON audit_log(fecha_hora DESC);

-- solicitudes_cambio_rubro: FK usada en findByEmpresaIdConDetalle
CREATE INDEX IF NOT EXISTS idx_solicitudes_cambio_rubro_empresa_id
    ON solicitudes_cambio_rubro(empresa_id);