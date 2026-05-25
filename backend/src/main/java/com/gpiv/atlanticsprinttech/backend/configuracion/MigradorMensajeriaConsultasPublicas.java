package com.gpiv.atlanticsprinttech.backend.configuracion;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura columnas de mensajería para compatibilidad cuando Flyway no está activo.
 */
@Component
@Order(1)
public class MigradorMensajeriaConsultasPublicas extends MigradorBasePostgresql {

    private static final Logger logger = LoggerFactory.getLogger(MigradorMensajeriaConsultasPublicas.class);

    public MigradorMensajeriaConsultasPublicas(DataSource datosConexion, JdbcTemplate plantillaJdbc) {
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
                      AND table_name = 'mensajeria_conversaciones'
                ) THEN
                    ALTER TABLE public.mensajeria_conversaciones
                        ADD COLUMN IF NOT EXISTS tipo_origen VARCHAR(20),
                        ADD COLUMN IF NOT EXISTS contacto_nombre_empresa VARCHAR(160),
                        ADD COLUMN IF NOT EXISTS contacto_correo_electronico VARCHAR(160),
                        ADD COLUMN IF NOT EXISTS contacto_telefono VARCHAR(40);

                    UPDATE public.mensajeria_conversaciones
                    SET tipo_origen = 'EMPRESA'
                    WHERE tipo_origen IS NULL;

                    ALTER TABLE public.mensajeria_conversaciones
                        ALTER COLUMN tipo_origen SET NOT NULL;

                    ALTER TABLE public.mensajeria_conversaciones
                        ALTER COLUMN empresa_id DROP NOT NULL;
                END IF;

                IF EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'mensajeria_mensajes'
                ) THEN
                    ALTER TABLE public.mensajeria_mensajes
                        ADD COLUMN IF NOT EXISTS emisor_externo_nombre VARCHAR(160);

                    ALTER TABLE public.mensajeria_mensajes
                        ALTER COLUMN usuario_emisor_id DROP NOT NULL;
                END IF;
            END $$;
            """);
    }
}



