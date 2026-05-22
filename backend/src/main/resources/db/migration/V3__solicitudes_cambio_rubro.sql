CREATE TABLE IF NOT EXISTS solicitudes_cambio_rubro (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresas(id),
    rubro_solicitado_id   BIGINT NOT NULL REFERENCES rubros(id),
    rubro_anterior_nombre VARCHAR(120),
    justificacion         VARCHAR(1000) NOT NULL,
    estado                VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo        VARCHAR(500),
    solicitado_por        VARCHAR(80)  NOT NULL,
    fecha_solicitud       TIMESTAMP    NOT NULL,
    resuelto_por          VARCHAR(80),
    fecha_resolucion      TIMESTAMP
);
