package com.gpiv.atlanticsprinttech.backend.configuracion;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura columnas de mensajeria para compatibilidad cuando Flyway no esta activo.
 */
@Component
@Order(1)
public class MigradorMensajeriaConsultasPublicas implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigradorMensajeriaConsultasPublicas.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public MigradorMensajeriaConsultasPublicas(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String producto = connection.getMetaData().getDatabaseProductName();
            if (producto == null || !producto.toLowerCase().contains("postgresql")) {
                return;
            }
        }

        jdbcTemplate.execute("""
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

        logger.info("Esquema de mensajeria verificado para consultas publicas");
    }
}

