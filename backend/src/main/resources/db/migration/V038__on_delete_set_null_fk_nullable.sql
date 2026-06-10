-- FKs anulables hacia usuarios: SET NULL al eliminar el usuario referenciado
ALTER TABLE proyectos_productivos
    DROP CONSTRAINT IF EXISTS proyectos_productivos_responsable_id_fkey,
    ADD CONSTRAINT proyectos_productivos_responsable_id_fkey
        FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL;

ALTER TABLE servicios
    DROP CONSTRAINT IF EXISTS servicios_ultimo_tecnico_id_fkey,
    ADD CONSTRAINT servicios_ultimo_tecnico_id_fkey
        FOREIGN KEY (ultimo_tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL;

ALTER TABLE mensajeria_conversaciones
    DROP CONSTRAINT IF EXISTS mensajeria_conversaciones_usuario_iniciador_id_fkey,
    ADD CONSTRAINT mensajeria_conversaciones_usuario_iniciador_id_fkey
        FOREIGN KEY (usuario_iniciador_id) REFERENCES usuarios(id) ON DELETE SET NULL;

-- FK anulable hacia empresas: SET NULL al eliminar la empresa referenciada
ALTER TABLE usuarios
    DROP CONSTRAINT IF EXISTS usuarios_empresa_id_fkey,
    ADD CONSTRAINT usuarios_empresa_id_fkey
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE SET NULL;