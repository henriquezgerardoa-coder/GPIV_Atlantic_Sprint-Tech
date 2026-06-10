-- Agrega soporte de declaración jurada al sistema de empleados
-- y formatea CUITs existentes con guiones

ALTER TABLE empleados
    ADD COLUMN declarado         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN fecha_declaracion DATE;

-- Formatea CUITs de 11 dígitos al formato XX-XXXXXXXX-X
UPDATE empleados
SET cuit = SUBSTRING(cuit, 1, 2) || '-' || SUBSTRING(cuit, 3, 8) || '-' || SUBSTRING(cuit, 11, 1)
WHERE LENGTH(cuit) = 11 AND cuit NOT LIKE '%-%';

CREATE INDEX idx_empleados_declarado ON empleados(empresa_id, declarado);
