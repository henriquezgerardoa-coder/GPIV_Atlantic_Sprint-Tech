package com.gpiv.atlanticsprinttech.backend.configuracion;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Clase base para migradores que sólo deben ejecutarse en PostgreSQL.
 * Centraliza la verificación del motor de base de datos y evita duplicación.
 * Las subclases implementan {@link #ejecutarMigracion()} con el SQL específico.
 */
public abstract class MigradorBasePostgresql implements CommandLineRunner {

    private static final String MOTOR_POSTGRESQL = "postgresql";

    protected final DataSource datosConexion;
    protected final JdbcTemplate plantillaJdbc;

    protected MigradorBasePostgresql(DataSource datosConexion, JdbcTemplate plantillaJdbc) {
        this.datosConexion = datosConexion;
        this.plantillaJdbc = plantillaJdbc;
    }

    @Override
    public final void run(String... args) throws Exception {
        if (!esPostgresql()) {
            return;
        }
        ejecutarMigracion();
        obtenerLogger().info("{} completado correctamente", getClass().getSimpleName());
    }

    /**
     * Implementa la lógica SQL de migración específica para cada subclase.
     */
    protected abstract void ejecutarMigracion();

    /**
     * Proporciona el logger del tipo concreto para mensajes contextualizados.
     */
    protected abstract Logger obtenerLogger();

    private boolean esPostgresql() throws Exception {
        try (Connection conexion = datosConexion.getConnection()) {
            String producto = conexion.getMetaData().getDatabaseProductName();
            return producto != null && producto.toLowerCase().contains(MOTOR_POSTGRESQL);
        }
    }
}

