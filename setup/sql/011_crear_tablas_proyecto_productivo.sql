-- ============================================================================
-- MIGRACIÓN: Crear tablas del dominio Proyecto Productivo
-- Sistema: GPIV Atlantic Sprint-Tech
-- Fecha: 2026-05-08
-- Versión: 011
-- ============================================================================
-- Propósito:
-- Incorpora el modelo de seguimiento de proyectos productivos que las empresas
-- radicadas ejecutan en sus lotes asignados.
--
-- Tablas creadas:
--   1. proyectos_productivos  - Proyecto de obra/producción vinculado a un lote
--   2. hitos_obra             - Hitos de avance del proyecto
--   3. documentos_pdf         - Documentación técnica del proyecto
-- ============================================================================

BEGIN;

\echo '*** INICIANDO MIGRACIÓN 011: Dominio Proyecto Productivo ***'

-- ============================================================================
-- 1. TABLA: proyectos_productivos
-- ============================================================================
CREATE TABLE IF NOT EXISTS proyectos_productivos (
    id                        BIGSERIAL        PRIMARY KEY,
    nombre                    VARCHAR(200)     NOT NULL,
    estado                    VARCHAR(30)      NOT NULL,
    descripcion               VARCHAR(2000)    NOT NULL,
    fecha_inicio_real         DATE,
    fecha_estimada_fin        DATE             NOT NULL,
    monto_inversion           NUMERIC(15, 2)   NOT NULL,
    fecha_creacion            TIMESTAMP        NOT NULL,

    empresa_id                BIGINT           NOT NULL,
    lote_id                   BIGINT           NOT NULL,
    responsable_seguimiento_id BIGINT          NOT NULL,
    radicacion_origen_id      BIGINT,

    CONSTRAINT fk_proyecto_empresa
        FOREIGN KEY (empresa_id)
        REFERENCES empresas(id),

    CONSTRAINT fk_proyecto_lote
        FOREIGN KEY (lote_id)
        REFERENCES lotes(id),

    CONSTRAINT fk_proyecto_responsable
        FOREIGN KEY (responsable_seguimiento_id)
        REFERENCES usuarios(id),

    CONSTRAINT fk_proyecto_radicacion
        FOREIGN KEY (radicacion_origen_id)
        REFERENCES radicaciones(id),

    CONSTRAINT chk_proyecto_estado
        CHECK (estado IN ('INICIADO', 'EN_EJECUCION', 'DETENIDO', 'COMPLETADO', 'CANCELADO'))
);

CREATE INDEX idx_proyecto_empresa   ON proyectos_productivos(empresa_id);
CREATE INDEX idx_proyecto_lote      ON proyectos_productivos(lote_id);
CREATE INDEX idx_proyecto_estado    ON proyectos_productivos(estado);

COMMENT ON TABLE  proyectos_productivos                          IS 'Proyectos productivos ejecutados por empresas en sus lotes asignados';
COMMENT ON COLUMN proyectos_productivos.estado                   IS 'INICIADO | EN_EJECUCION | DETENIDO | COMPLETADO | CANCELADO';
COMMENT ON COLUMN proyectos_productivos.fecha_inicio_real        IS 'Se fija cuando el proyecto transiciona a EN_EJECUCION';
COMMENT ON COLUMN proyectos_productivos.radicacion_origen_id     IS 'Expediente de radicacion aprobada que origina el proyecto; nullable hasta llamar iniciarProyecto()';

\echo 'PASO 1: Tabla proyectos_productivos creada'

-- ============================================================================
-- 2. TABLA: hitos_obra
-- ============================================================================
CREATE TABLE IF NOT EXISTS hitos_obra (
    id                    BIGSERIAL    PRIMARY KEY,
    descripcion           VARCHAR(500) NOT NULL,
    fecha_vencimiento_real DATE        NOT NULL,
    cumplido              BOOLEAN      NOT NULL DEFAULT FALSE,

    proyecto_id           BIGINT       NOT NULL,

    CONSTRAINT fk_hito_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyectos_productivos(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_hito_proyecto  ON hitos_obra(proyecto_id);
CREATE INDEX idx_hito_cumplido  ON hitos_obra(cumplido);

COMMENT ON TABLE  hitos_obra                       IS 'Hitos de avance de obra de un proyecto productivo';
COMMENT ON COLUMN hitos_obra.cumplido              IS 'true una vez que se llama marcaComoCumplido(); no retrocede';
COMMENT ON COLUMN hitos_obra.fecha_vencimiento_real IS 'Plazo comprometido del hito; usado por estaVencido()';

\echo 'PASO 2: Tabla hitos_obra creada'

-- ============================================================================
-- 3. TABLA: documentos_pdf
-- ============================================================================
CREATE TABLE IF NOT EXISTS documentos_pdf (
    id             BIGSERIAL     PRIMARY KEY,
    nombre_archivo VARCHAR(200)  NOT NULL,
    fecha_carga    TIMESTAMP     NOT NULL,
    tamano_bytes   BIGINT        NOT NULL,
    contenido      BYTEA         NOT NULL,

    proyecto_id    BIGINT        NOT NULL,

    CONSTRAINT fk_documento_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyectos_productivos(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_documento_extension
        CHECK (lower(nombre_archivo) LIKE '%.pdf'),

    CONSTRAINT chk_documento_tamano
        CHECK (tamano_bytes > 0 AND tamano_bytes <= 20971520)
);

CREATE INDEX idx_documento_proyecto ON documentos_pdf(proyecto_id);

COMMENT ON TABLE  documentos_pdf               IS 'Documentación técnica PDF adjunta a un proyecto productivo';
COMMENT ON COLUMN documentos_pdf.tamano_bytes  IS 'Límite de 20 MB (20971520 bytes); espejado de la validación en DocumentoPDF.java';
COMMENT ON COLUMN documentos_pdf.contenido     IS 'Binario del archivo PDF';

\echo 'PASO 3: Tabla documentos_pdf creada'

-- ============================================================================
-- 4. REGISTRO EN schema_version
-- ============================================================================
INSERT INTO schema_version (version, descripcion, tipo_cambio, fecha_aplicacion, estado)
VALUES (
    '011',
    'Crear tablas proyectos_productivos, hitos_obra y documentos_pdf',
    'CREATE_TABLE',
    CURRENT_TIMESTAMP,
    'COMPLETADO'
) ON CONFLICT DO NOTHING;

\echo '*** MIGRACIÓN 011 COMPLETADA EXITOSAMENTE ***'

COMMIT;

-- Validación post-migración
SELECT table_name, COUNT(*) AS columnas
FROM information_schema.columns
WHERE table_name IN ('proyectos_productivos', 'hitos_obra', 'documentos_pdf')
  AND table_schema = 'public'
GROUP BY table_name
ORDER BY table_name;
