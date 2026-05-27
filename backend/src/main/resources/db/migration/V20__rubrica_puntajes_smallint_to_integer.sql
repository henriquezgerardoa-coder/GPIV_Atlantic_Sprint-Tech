ALTER TABLE solicitudes_cambio_rubro
    ALTER COLUMN rubrica_puntaje_impacto_operativo TYPE INTEGER;

ALTER TABLE solicitudes_cambio_rubro
    ALTER COLUMN rubrica_puntaje_compatibilidad_parque TYPE INTEGER;

ALTER TABLE solicitudes_cambio_rubro
    ALTER COLUMN rubrica_puntaje_viabilidad_tecnica TYPE INTEGER;

ALTER TABLE solicitudes_cambio_rubro
    ALTER COLUMN rubrica_puntaje_cumplimiento_normativo TYPE INTEGER;