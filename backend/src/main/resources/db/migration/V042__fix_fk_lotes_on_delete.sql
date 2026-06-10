-- FK radicaciones.lote_id: sin acción definida → bloquea DELETE en lotes.
-- Como lote_id es anulable, al eliminar el lote se anula la referencia.
ALTER TABLE radicaciones
    DROP CONSTRAINT IF EXISTS radicaciones_lote_id_fkey,
    ADD CONSTRAINT radicaciones_lote_id_fkey
        FOREIGN KEY (lote_id) REFERENCES lotes(id) ON DELETE SET NULL;

-- FK lotes.parent_lote_id (fk_lotes_parent): sin acción definida → bloquea DELETE
-- del lote padre si existen sublotes. Con SET NULL los sublotes pasan a ser principales.
ALTER TABLE lotes
    DROP CONSTRAINT IF EXISTS fk_lotes_parent,
    ADD CONSTRAINT fk_lotes_parent
        FOREIGN KEY (parent_lote_id) REFERENCES lotes(id) ON DELETE SET NULL;