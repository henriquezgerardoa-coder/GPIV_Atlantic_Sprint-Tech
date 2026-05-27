package com.gpiv.atlanticsprinttech.backend.configuracion;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Crea la tabla de evaluaciones por etapas (HU-03) cuando Flyway no está activo.
 */
@Component
@Order(4)
public class MigradorTablaEvaluacionRadicacion extends MigradorBasePostgresql {

    private static final Logger logger = LoggerFactory.getLogger(MigradorTablaEvaluacionRadicacion.class);

    public MigradorTablaEvaluacionRadicacion(DataSource datosConexion, JdbcTemplate plantillaJdbc) {
        super(datosConexion, plantillaJdbc);
    }

    @Override
    protected Logger obtenerLogger() {
        return logger;
    }

    @Override
    protected void ejecutarMigracion() {
        plantillaJdbc.execute("""
            CREATE TABLE IF NOT EXISTS evaluaciones_radicacion (
                id                        BIGSERIAL PRIMARY KEY,
                radicacion_id             BIGINT NOT NULL UNIQUE
                                              REFERENCES radicaciones(id),
                etapa1_empleo_directo     INTEGER CHECK (etapa1_empleo_directo BETWEEN 1 AND 5),
                etapa1_materia_prima_local INTEGER CHECK (etapa1_materia_prima_local BETWEEN 1 AND 5),
                etapa1_impacto_ambiental  INTEGER CHECK (etapa1_impacto_ambiental BETWEEN 1 AND 5),
                etapa1_observaciones      VARCHAR(500),
                etapa1_fecha_guardado     TIMESTAMP,
                etapa2_rentabilidad       INTEGER CHECK (etapa2_rentabilidad BETWEEN 1 AND 5),
                etapa2_solidez_financiera INTEGER CHECK (etapa2_solidez_financiera BETWEEN 1 AND 5),
                etapa2_inversion_declarada INTEGER CHECK (etapa2_inversion_declarada BETWEEN 1 AND 5),
                etapa2_observaciones      VARCHAR(500),
                etapa2_fecha_guardado     TIMESTAMP,
                etapa3_viabilidad_tecnica INTEGER CHECK (etapa3_viabilidad_tecnica BETWEEN 1 AND 5),
                etapa3_cronograma_obra    INTEGER CHECK (etapa3_cronograma_obra BETWEEN 1 AND 5),
                etapa3_calidad_documentacion INTEGER CHECK (etapa3_calidad_documentacion BETWEEN 1 AND 5),
                etapa3_observaciones      VARCHAR(500),
                etapa3_fecha_guardado     TIMESTAMP,
                fecha_creacion            TIMESTAMP NOT NULL,
                fecha_actualizacion       TIMESTAMP NOT NULL,
                evaluador                 VARCHAR(80)
            );
            """);
    }
}