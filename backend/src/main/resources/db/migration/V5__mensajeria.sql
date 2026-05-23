-- V5: Mensajeria entre empresa y admin/directivo

CREATE TABLE IF NOT EXISTS mensajeria_conversaciones (
    id                       BIGSERIAL PRIMARY KEY,
    empresa_id               BIGINT      NOT NULL REFERENCES empresas(id),
    usuario_responsable_id   BIGINT      NOT NULL REFERENCES usuarios(id),
    asunto                   VARCHAR(160) NOT NULL,
    fecha_creacion           TIMESTAMP   NOT NULL,
    fecha_ultima_actualizacion TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS mensajeria_mensajes (
    id                BIGSERIAL PRIMARY KEY,
    conversacion_id   BIGINT       NOT NULL REFERENCES mensajeria_conversaciones(id),
    usuario_emisor_id BIGINT       NOT NULL REFERENCES usuarios(id),
    contenido         VARCHAR(4000) NOT NULL,
    fecha_envio       TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mensajeria_conversaciones_empresa_fecha
    ON mensajeria_conversaciones (empresa_id, fecha_ultima_actualizacion DESC);

CREATE INDEX IF NOT EXISTS idx_mensajeria_mensajes_conversacion_fecha
    ON mensajeria_mensajes (conversacion_id, fecha_envio ASC);

