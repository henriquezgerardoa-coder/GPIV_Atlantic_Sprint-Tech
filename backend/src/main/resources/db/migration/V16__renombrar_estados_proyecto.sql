-- Alinea los valores de estado con el diagrama de clases del dominio
UPDATE proyectos_productivos SET estado = 'INICIADO'   WHERE estado = 'PLANIFICADO';
UPDATE proyectos_productivos SET estado = 'DETENIDO'   WHERE estado = 'PAUSADO';
UPDATE proyectos_productivos SET estado = 'COMPLETADO' WHERE estado = 'FINALIZADO';