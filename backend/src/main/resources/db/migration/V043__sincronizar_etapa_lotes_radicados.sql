-- Sincroniza la etapa de lotes ocupados cuya radicacion ya se encuentra en estado RADICADA.
-- Corrige inconsistencias históricas: la lógica de sincronización automática RADICADA → OPERATIVO
-- se implementó en Junio 2026; los lotes radicados antes de ese cambio mantenían su etapa anterior.
UPDATE lotes
SET etapa = 'OPERATIVO',
    fecha_inicio_etapa_actual = CURRENT_DATE
WHERE ocupado = true
  AND etapa NOT IN ('OPERATIVO', 'ESCRITURADO', 'REVERTIDO', 'DISPONIBLE')
  AND id IN (
      SELECT DISTINCT lote_id
      FROM radicaciones
      WHERE estado = 'RADICADA'
        AND lote_id IS NOT NULL
  );