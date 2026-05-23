-- Migración: Crear tabla de empleados
-- Permite que cada empresa cargue CUIT y nombre de sus empleados

CREATE TABLE empleados (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cuit VARCHAR(20) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    empresa_id BIGINT NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_empleado_empresa_cuit UNIQUE (empresa_id, cuit),
    CONSTRAINT fk_empleado_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear índices para optimizar búsquedas
CREATE INDEX idx_empleado_empresa_id ON empleados(empresa_id);
CREATE INDEX idx_empleado_cuit ON empleados(cuit);

