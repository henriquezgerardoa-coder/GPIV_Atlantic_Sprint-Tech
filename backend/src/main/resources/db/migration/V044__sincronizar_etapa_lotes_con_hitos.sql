-- Sincroniza la etapa de lotes en ADJUDICADO_PRECARIO que ya tienen hitos de obra activos.
-- Corrige inconsistencias históricas: la transición automática ADJUDICADO_PRECARIO → EN_CONSTRUCCION
-- al crear el primer hito se implementó en Junio 2026.
UPDATE lotes
SET etapa = 'EN_CONSTRUCCION',
    fecha_inicio_etapa_actual = CURRENT_DATE
WHERE etapa = 'ADJUDICADO_PRECARIO'
  AND id IN (
      SELECT DISTINCT r.lote_id
      FROM radicaciones r
      JOIN proyectos_productivos p ON p.solicitud_origen_id = r.id
      JOIN hitos_obra h ON h.proyecto_id = p.id
      WHERE r.lote_id IS NOT NULL
        AND h.cumplido = false
  );