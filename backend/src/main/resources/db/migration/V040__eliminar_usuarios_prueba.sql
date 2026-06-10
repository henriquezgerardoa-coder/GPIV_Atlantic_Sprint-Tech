-- Eliminación de usuarios de prueba y datos relacionados.
-- Usuarios: Fede, Emi, Usuario, empPrueba.
-- Empresa: la vinculada al usuario 'Fede', con todos sus datos dependientes.

DO $$
DECLARE
    v_empresa_id BIGINT;
BEGIN
    SELECT empresa_id INTO v_empresa_id
    FROM usuarios WHERE nombre_usuario = 'Fede' LIMIT 1;

    IF v_empresa_id IS NOT NULL THEN
        -- Hitos de proyectos ligados a radicaciones de esta empresa
        DELETE FROM hitos_obra WHERE proyecto_id IN (
            SELECT id FROM proyectos_productivos
            WHERE solicitud_origen_id IN (
                SELECT id FROM radicaciones WHERE empresa_id = v_empresa_id
            )
        );

        -- Mensajería
        DELETE FROM mensajeria_mensajes WHERE conversacion_id IN (
            SELECT id FROM mensajeria_conversaciones WHERE empresa_id = v_empresa_id
        );
        DELETE FROM mensajeria_conversaciones WHERE empresa_id = v_empresa_id;

        -- Evaluaciones de radicaciones
        DELETE FROM evaluaciones_radicacion WHERE radicacion_id IN (
            SELECT id FROM radicaciones WHERE empresa_id = v_empresa_id
        );

        -- Documentos de radicaciones
        DELETE FROM radicacion_documentos WHERE radicacion_id IN (
            SELECT id FROM radicaciones WHERE empresa_id = v_empresa_id
        );

        -- Historial de radicaciones
        DELETE FROM radicacion_historial WHERE radicacion_id IN (
            SELECT id FROM radicaciones WHERE empresa_id = v_empresa_id
        );

        -- Proyectos productivos
        DELETE FROM proyectos_productivos WHERE solicitud_origen_id IN (
            SELECT id FROM radicaciones WHERE empresa_id = v_empresa_id
        );

        -- Solicitudes de cambio de rubro
        DELETE FROM solicitudes_cambio_rubro WHERE empresa_id = v_empresa_id;

        -- Vehículos
        DELETE FROM vehiculos WHERE empresa_id = v_empresa_id;

        -- Personal registrado y censos
        DELETE FROM personal_registrado WHERE empresa_id = v_empresa_id;
        DELETE FROM censos WHERE empresa_id = v_empresa_id;

        -- Empleados
        DELETE FROM empleados WHERE empresa_id = v_empresa_id;

        -- Radicaciones
        DELETE FROM radicaciones WHERE empresa_id = v_empresa_id;

        -- Lotes: nullear empresa_id sin eliminar el lote
        UPDATE lotes SET empresa_id = NULL WHERE empresa_id = v_empresa_id;

        -- Eliminar la empresa
        DELETE FROM empresas WHERE id = v_empresa_id;
    END IF;

    -- Mensajería donde los usuarios a eliminar son responsables o emisores (cualquier empresa)
    DELETE FROM mensajeria_mensajes WHERE conversacion_id IN (
        SELECT id FROM mensajeria_conversaciones
        WHERE usuario_responsable_id IN (
            SELECT id FROM usuarios WHERE nombre_usuario IN ('Fede', 'Emi', 'Usuario', 'empPrueba')
        )
    );
    DELETE FROM mensajeria_conversaciones WHERE usuario_responsable_id IN (
        SELECT id FROM usuarios WHERE nombre_usuario IN ('Fede', 'Emi', 'Usuario', 'empPrueba')
    );
    DELETE FROM mensajeria_mensajes WHERE usuario_emisor_id IN (
        SELECT id FROM usuarios WHERE nombre_usuario IN ('Fede', 'Emi', 'Usuario', 'empPrueba')
    );

    -- Roles y usuarios de prueba
    DELETE FROM usuarios_roles WHERE usuario_id IN (
        SELECT id FROM usuarios
        WHERE nombre_usuario IN ('Fede', 'Emi', 'Usuario', 'empPrueba')
    );
    DELETE FROM usuarios
    WHERE nombre_usuario IN ('Fede', 'Emi', 'Usuario', 'empPrueba');
END $$;
