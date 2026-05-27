-- HU-04: número de resolución/dictamen y usuario que resuelve el expediente
ALTER TABLE radicaciones
    ADD COLUMN IF NOT EXISTS numero_resolucion VARCHAR(50),
    ADD COLUMN IF NOT EXISTS resuelto_por      VARCHAR(80);