package com.gpiv.atlanticsprinttech.backend.configuracion;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Agrega columna de consumos post-radicación cuando Flyway no está activo.
 */
@Component
@Order(3)
public class MigradorServiciosPostRadicacionConsumos extends MigradorBasePostgresql {

    private static final Logger logger = LoggerFactory.getLogger(MigradorServiciosPostRadicacionConsumos.class);

    public MigradorServiciosPostRadicacionConsumos(DataSource datosConexion, JdbcTemplate plantillaJdbc) {
        super(datosConexion, plantillaJdbc);
    }

    @Override
    protected Logger obtenerLogger() {
        return logger;
    }

    @Override
    protected void ejecutarMigracion() {
        plantillaJdbc.execute("""
            ALTER TABLE public.empresas
                ADD COLUMN IF NOT EXISTS servicios_post_radicacion_json VARCHAR(20000);
            """);
    }
}
