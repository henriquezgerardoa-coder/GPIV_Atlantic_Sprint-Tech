CREATE TABLE IF NOT EXISTS empleados (
    id             BIGSERIAL    PRIMARY KEY,
    cuit           VARCHAR(20)  NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    fecha_registro TIMESTAMP    NOT NULL,
    empresa_id     BIGINT       NOT NULL REFERENCES empresas(id),
    CONSTRAINT uk_empleado_empresa_cuit UNIQUE (empresa_id, cuit)
);