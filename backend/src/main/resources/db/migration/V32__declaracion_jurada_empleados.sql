-- Declaración jurada semestral de empleados
-- Agrega estado declarado a personal_registrado y soporte semestral a censos

ALTER TABLE personal_registrado
    ADD COLUMN declarado        BOOLEAN  NOT NULL DEFAULT FALSE,
    ADD COLUMN fecha_declaracion DATE;

-- Agrega semestre a censos (0 = legacy anual, 1 = primer semestre, 2 = segundo semestre)
ALTER TABLE censos ADD COLUMN semestre SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE censos DROP CONSTRAINT uk_censo_empresa_periodo;
ALTER TABLE censos ADD CONSTRAINT uk_censo_empresa_periodo_sem
    UNIQUE (empresa_id, anio_periodo, semestre);

CREATE INDEX idx_personal_declarado ON personal_registrado(empresa_id, declarado);
