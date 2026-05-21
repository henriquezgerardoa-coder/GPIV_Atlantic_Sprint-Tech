-- V1: Schema inicial - todas las tablas del dominio GPIV
-- Usa IF NOT EXISTS para ser idempotente sobre DBs que ya tengan tablas parciales.

CREATE TABLE IF NOT EXISTS rubros (
    id                       BIGSERIAL PRIMARY KEY,
    nombre                   VARCHAR(120) NOT NULL,
    descripcion              VARCHAR(500),
    requiere_permiso_especial BOOLEAN     NOT NULL,
    CONSTRAINT uk_rubro_nombre UNIQUE (nombre)
);

CREATE TABLE IF NOT EXISTS sectores (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    CONSTRAINT uk_sector_nombre UNIQUE (nombre)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id                   BIGSERIAL PRIMARY KEY,
    fecha_hora           TIMESTAMP   NOT NULL,
    usuario_responsable  VARCHAR(120) NOT NULL,
    operacion            VARCHAR(80)  NOT NULL,
    entidad_afectada     VARCHAR(80)  NOT NULL,
    id_referencia        VARCHAR(40),
    valor_anterior       VARCHAR(2000),
    valor_nuevo          VARCHAR(2000),
    direccion_ip         VARCHAR(45)
);

CREATE TABLE IF NOT EXISTS empresas (
    id                      BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(120) NOT NULL,
    razon_social            VARCHAR(160) NOT NULL,
    nit                     VARCHAR(30)  NOT NULL,
    cuit                    VARCHAR(20)  NOT NULL,
    direccion               VARCHAR(240) NOT NULL,
    actividad_economica     VARCHAR(180) NOT NULL,
    correo_electronico      VARCHAR(120) NOT NULL,
    telefono                VARCHAR(40),
    fecha_registro          TIMESTAMP   NOT NULL,
    status                  VARCHAR(30)  NOT NULL,
    cantidad_empleados      INTEGER,
    vehiculos_asignados_json VARCHAR(12000),
    rubro_id                BIGINT REFERENCES rubros(id),
    CONSTRAINT uk_empresa_cuit UNIQUE (cuit),
    CONSTRAINT uk_empresa_nit  UNIQUE (nit)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id                          BIGSERIAL PRIMARY KEY,
    nombre_usuario              VARCHAR(60)  NOT NULL,
    nombre_completo             VARCHAR(120) NOT NULL,
    correo_electronico          VARCHAR(160),
    clave_acceso_hash           VARCHAR(120) NOT NULL,
    activo                      BOOLEAN      NOT NULL,
    email_verificado            BOOLEAN,
    token_verificacion_email    VARCHAR(120),
    token_verificacion_expira_en TIMESTAMP,
    fecha_verificacion_email    TIMESTAMP,
    fecha_ultimo_acceso         TIMESTAMP,
    empresa_id                  BIGINT REFERENCES empresas(id),
    CONSTRAINT uk_usuario_nombre_usuario UNIQUE (nombre_usuario)
);

CREATE TABLE IF NOT EXISTS usuarios_roles (
    usuario_id BIGINT      NOT NULL REFERENCES usuarios(id),
    rol        VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS lotes (
    id                           BIGSERIAL PRIMARY KEY,
    codigo                       VARCHAR(40) NOT NULL,
    superficie_m2                DOUBLE PRECISION NOT NULL,
    ocupado                      BOOLEAN     NOT NULL,
    empresa_id                   BIGINT REFERENCES empresas(id),
    fecha_asignacion             DATE,
    estado_asignacion            VARCHAR(40),
    numero_expediente_referencia VARCHAR(40),
    zona                         VARCHAR(20),
    CONSTRAINT uk_lote_empresa_codigo UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS radicaciones (
    id                          BIGSERIAL PRIMARY KEY,
    numero_radicado             VARCHAR(40)   NOT NULL,
    empresa_id                  BIGINT        NOT NULL REFERENCES empresas(id),
    lote_id                     BIGINT REFERENCES lotes(id),
    tipo_solicitud              VARCHAR(80)   NOT NULL,
    descripcion                 VARCHAR(1000) NOT NULL,
    uso_estimativo              VARCHAR(120),
    relevamiento_pedido_lotes   VARCHAR(12000),
    proyecto_productivo         VARCHAR(120),
    rentabilidad_estimada       DOUBLE PRECISION,
    empleados_previstos         INTEGER,
    plan_ambiental              VARCHAR(500),
    tiempo_estimado_obra_meses  INTEGER,
    fecha_plazo                 DATE,
    fecha_aprobacion            DATE,
    fecha_radicacion            DATE          NOT NULL,
    estado                      VARCHAR(40)   NOT NULL,
    fecha_ultima_actualizacion  TIMESTAMP     NOT NULL,
    CONSTRAINT uk_radicacion_numero UNIQUE (numero_radicado)
);

CREATE TABLE IF NOT EXISTS radicacion_historial (
    id           BIGSERIAL PRIMARY KEY,
    radicacion_id BIGINT       NOT NULL REFERENCES radicaciones(id),
    estado       VARCHAR(40)   NOT NULL,
    comentario   VARCHAR(1000),
    usuario      VARCHAR(80)   NOT NULL,
    fecha_evento TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS radicacion_documentos (
    id             BIGSERIAL PRIMARY KEY,
    radicacion_id  BIGINT       NOT NULL REFERENCES radicaciones(id),
    tipo_documento VARCHAR(20)  NOT NULL,
    nombre_archivo VARCHAR(200) NOT NULL,
    mime_type      VARCHAR(120) NOT NULL,
    tamano_bytes   BIGINT       NOT NULL,
    descripcion    VARCHAR(250),
    subido_por     VARCHAR(80)  NOT NULL,
    fecha_subida   TIMESTAMP    NOT NULL,
    contenido      BYTEA        NOT NULL
);

CREATE TABLE IF NOT EXISTS censos (
    id                            BIGSERIAL PRIMARY KEY,
    empresa_id                    BIGINT  NOT NULL REFERENCES empresas(id),
    fecha_declaracion             DATE    NOT NULL,
    anio_periodo                  INTEGER NOT NULL,
    cantidad_personal_registrado  INTEGER NOT NULL,
    cantidad_personas_no_registrado INTEGER NOT NULL,
    observacion                   VARCHAR(500),
    CONSTRAINT uk_censo_empresa_periodo UNIQUE (empresa_id, anio_periodo)
);

CREATE TABLE IF NOT EXISTS personal_registrado (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT       NOT NULL REFERENCES empresas(id),
    cuit            VARCHAR(20)  NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    fecha_ingreso   DATE         NOT NULL,
    CONSTRAINT uk_personal_empresa_cuit UNIQUE (empresa_id, cuit)
);

CREATE TABLE IF NOT EXISTS vehiculos (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT       NOT NULL REFERENCES empresas(id),
    patente      VARCHAR(15)  NOT NULL,
    marca_modelo VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS proyectos_productivos (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(160)   NOT NULL,
    estado              VARCHAR(40)    NOT NULL,
    descripcion         VARCHAR(1000)  NOT NULL,
    fecha_inicio_real   DATE,
    fecha_estimada_fin  DATE,
    monto_inversion     NUMERIC(15, 2),
    fecha_creacion      TIMESTAMP      NOT NULL,
    responsable_id      BIGINT REFERENCES usuarios(id),
    solicitud_origen_id BIGINT REFERENCES radicaciones(id)
);

CREATE TABLE IF NOT EXISTS hitos_obra (
    id                    BIGSERIAL PRIMARY KEY,
    proyecto_id           BIGINT       NOT NULL REFERENCES proyectos_productivos(id),
    descripcion           VARCHAR(300) NOT NULL,
    fecha_vencimiento_real DATE,
    cumplido              BOOLEAN      NOT NULL
);

CREATE TABLE IF NOT EXISTS documentos_pdf (
    id             BIGSERIAL PRIMARY KEY,
    proyecto_id    BIGINT       NOT NULL REFERENCES proyectos_productivos(id),
    nombre_archivo VARCHAR(255) NOT NULL,
    fecha_carga    TIMESTAMP    NOT NULL,
    tamano_bytes   BIGINT
);

CREATE TABLE IF NOT EXISTS servicios (
    id                   BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(120) NOT NULL,
    estado_actual        VARCHAR(40)  NOT NULL,
    descripcion_tecnica  VARCHAR(1000),
    ultimo_comentario    VARCHAR(500),
    ultimo_tecnico_id    BIGINT REFERENCES usuarios(id)
);