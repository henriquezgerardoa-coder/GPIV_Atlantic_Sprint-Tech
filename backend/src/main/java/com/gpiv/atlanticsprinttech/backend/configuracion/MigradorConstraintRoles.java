package com.gpiv.atlanticsprinttech.backend.configuracion;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajusta el constraint legacy de roles para permitir EMPRESA antes de cargar semillas.
 */
@Component
@Order(0)
public class MigradorConstraintRoles extends MigradorBasePostgresql {

    private static final Logger logger = LoggerFactory.getLogger(MigradorConstraintRoles.class);

    public MigradorConstraintRoles(DataSource datosConexion, JdbcTemplate plantillaJdbc) {
        super(datosConexion, plantillaJdbc);
    }

    @Override
    protected Logger obtenerLogger() {
        return logger;
    }

    @Override
    protected void ejecutarMigracion() {
        plantillaJdbc.execute("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'usuarios_roles'
                ) THEN
                    IF EXISTS (
                        SELECT 1
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        WHERE n.nspname = 'public'
                          AND t.relname = 'usuarios_roles'
                          AND c.conname = 'usuarios_roles_rol_check'
                    ) THEN
                        ALTER TABLE public.usuarios_roles DROP CONSTRAINT usuarios_roles_rol_check;
                    END IF;

                    UPDATE public.usuarios_roles
                    SET rol = 'EMPRESA'
                    WHERE UPPER(rol) IN ('EMPRESA_CONSULTORA', 'CLIENTE');

                    UPDATE public.usuarios_roles
                    SET rol = 'DIRECTIVO'
                    WHERE UPPER(rol) = 'OPERADOR';

                    BEGIN
                        ALTER TABLE public.usuarios_roles
                        ADD CONSTRAINT usuarios_roles_rol_check
                        CHECK (rol IN ('ADMINISTRADOR', 'DIRECTIVO', 'EMPRESA', 'SECRETARIO', 'TECNICO'));
                    EXCEPTION
                        WHEN duplicate_object THEN NULL;
                    END;
                END IF;
            END $$;
            """);
    }
}
