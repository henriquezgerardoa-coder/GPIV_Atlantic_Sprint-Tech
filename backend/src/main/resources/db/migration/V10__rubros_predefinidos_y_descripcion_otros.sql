-- Rubros predefinidos del parque industrial
INSERT INTO rubros (nombre, descripcion, requiere_permiso_especial) VALUES
    ('Manufactura y producción',                    NULL, false),
    ('Logística y almacenamiento',                  NULL, false),
    ('Industria ligera y ensamblaje',               NULL, false),
    ('Agroindustrial y alimentación',               NULL, false),
    ('Química y farmacéutica',                      NULL, true),
    ('Metalurgia y siderurgia',                     NULL, false),
    ('Servicios de energía, redes hídricas y cloacales', NULL, false),
    ('Infraestructura vial y conectividad',         NULL, false),
    ('Telecomunicaciones',                          NULL, false),
    ('Seguridad y control',                         NULL, false),
    ('Servicios administrativos y comunes',         NULL, false),
    ('Otros',                                       NULL, false)
ON CONFLICT (nombre) DO NOTHING;

-- Campo para descripción libre cuando el rubro seleccionado es "Otros"
ALTER TABLE solicitudes_cambio_rubro
    ADD COLUMN IF NOT EXISTS descripcion_otros VARCHAR(300);