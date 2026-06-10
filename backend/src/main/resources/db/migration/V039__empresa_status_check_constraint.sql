ALTER TABLE empresas
    ADD CONSTRAINT chk_empresa_status CHECK (status IN ('ACTIVA', 'INACTIVA'));