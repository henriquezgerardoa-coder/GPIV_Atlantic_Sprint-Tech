-- V26: Normalizar estado FINALIZADO → COMPLETADO en proyectos_productivos
-- El enum EstadoProyecto no incluye FINALIZADO (fue renombrado en V16).
-- Proyectos insertados con estado histórico quedaron con el valor antiguo.
UPDATE proyectos_productivos SET estado = 'COMPLETADO' WHERE estado = 'FINALIZADO';