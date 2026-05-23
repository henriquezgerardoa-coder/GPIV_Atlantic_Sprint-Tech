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
 * Agrega columna de consumos post-radicacion cuando Flyway no está activo.
 */
@Component
@Order(3)
public class MigradorServiciosPostRadicacionConsumos implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigradorServiciosPostRadicacionConsumos.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public MigradorServiciosPostRadicacionConsumos(DataSource dataSource, JdbcTemplate jdbcTemplate) {
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
            ALTER TABLE public.empresas
                ADD COLUMN IF NOT EXISTS servicios_post_radicacion_json VARCHAR(20000);
            """);

        logger.info("Esquema de servicios post-radicacion verificado para consumos");
    }
}

