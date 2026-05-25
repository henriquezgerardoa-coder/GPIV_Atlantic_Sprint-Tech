-- Rubros predefinidos del parque industrial
INSERT INTO rubros (nombre, descripcion, requiere_permiso_especial)
SELECT datos.nombre, datos.descripcion, datos.requiere_permiso_especial
FROM (
    SELECT 'Manufactura y producción' AS nombre, NULL AS descripcion, false AS requiere_permiso_especial
    UNION ALL SELECT 'Logística y almacenamiento', NULL, false
    UNION ALL SELECT 'Industria ligera y ensamblaje', NULL, false
    UNION ALL SELECT 'Agroindustrial y alimentación', NULL, false
    UNION ALL SELECT 'Química y farmacéutica', NULL, true
    UNION ALL SELECT 'Metalurgia y siderurgia', NULL, false
    UNION ALL SELECT 'Servicios de energía, redes hídricas y cloacales', NULL, false
    UNION ALL SELECT 'Infraestructura vial y conectividad', NULL, false
    UNION ALL SELECT 'Telecomunicaciones', NULL, false
    UNION ALL SELECT 'Seguridad y control', NULL, false
    UNION ALL SELECT 'Servicios administrativos y comunes', NULL, false
    UNION ALL SELECT 'Otros', NULL, false
) datos
WHERE NOT EXISTS (
    SELECT 1 FROM rubros r WHERE r.nombre = datos.nombre
);

-- Campo para descripción libre cuando el rubro seleccionado es "Otros"
ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS descripcion_otros VARCHAR(300);