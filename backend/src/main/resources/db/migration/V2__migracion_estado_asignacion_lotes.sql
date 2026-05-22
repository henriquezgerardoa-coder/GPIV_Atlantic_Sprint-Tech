-- Migración de valores legacy del enum EstadoAsignacionLote
UPDATE lotes SET estado_asignacion = 'ADJUDICADO'   WHERE estado_asignacion = 'ADJUDICADO_RADICADA';
UPDATE lotes SET estado_asignacion = 'DESADJUDICADO' WHERE estado_asignacion = 'RESERVADO';