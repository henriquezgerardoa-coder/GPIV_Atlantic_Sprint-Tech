-- Los lotes en etapa REVERTIDO deben tener ocupado=false y sin empresa asignada.
-- Corrige registros existentes que quedaron con empresa asignada al revertirse.
UPDATE lotes
SET ocupado              = false,
    empresa_id           = NULL,
    fecha_asignacion     = NULL,
    numero_expediente_referencia = NULL
WHERE etapa = 'REVERTIDO'
  AND (ocupado = true OR empresa_id IS NOT NULL);