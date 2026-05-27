-- Agrega el usuario que inicia la conversación (para mensajería entre usuarios)
ALTER TABLE mensajeria_conversaciones
    ADD COLUMN usuario_iniciador_id BIGINT REFERENCES usuarios(id);
