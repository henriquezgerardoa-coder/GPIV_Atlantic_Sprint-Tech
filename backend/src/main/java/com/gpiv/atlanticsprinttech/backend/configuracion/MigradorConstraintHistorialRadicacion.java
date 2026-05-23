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
 * Corrige el constraint radicacion_historial_estado_check para incluir RADICADA
 * como valor válido, en entornos donde Flyway no está activo.
 */
@Component
@Order(2)
public class MigradorConstraintHistorialRadicacion implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigradorConstraintHistorialRadicacion.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public MigradorConstraintHistorialRadicacion(DataSource dataSource, JdbcTemplate jdbcTemplate) {
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
            DECLARE
                constraint_valido BOOLEAN;
            BEGIN
                -- Verificar si el constraint ya incluye RADICADA
                SELECT pg_get_constraintdef(c.oid) LIKE '%RADICADA%'
                INTO constraint_valido
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                WHERE c.conname = 'radicacion_historial_estado_check'
                  AND t.relname = 'radicacion_historial';

                IF constraint_valido IS NULL OR NOT constraint_valido THEN
                    ALTER TABLE radicacion_historial
                        DROP CONSTRAINT IF EXISTS radicacion_historial_estado_check;

                    ALTER TABLE radicacion_historial
                        DROP CONSTRAINT IF EXISTS radicacion_historial_estado_anterior_check;

                    ALTER TABLE radicacion_historial
                        ADD CONSTRAINT radicacion_historial_estado_check
                            CHECK (estado IN (
                                'PENDIENTE',
                                'EN_REVISION',
                                'APROBADA',
                                'RADICADA',
                                'RECHAZADA',
                                'REQUIERE_INFORMACION_ADICIONAL',
                                'CANCELADA'
                            ));

                    RAISE NOTICE 'Constraint radicacion_historial_estado_check corregido para incluir RADICADA';
                END IF;
            END $$;
            """);

        logger.info("Constraint radicacion_historial_estado_check verificado y corregido si era necesario");
    }
}

