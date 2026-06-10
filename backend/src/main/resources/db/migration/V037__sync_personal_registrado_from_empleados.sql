-- V037: Sincronizar PersonalRegistrado desde tabla empleados
-- Inserta registros de personal_registrado para empleados que ya se encuentran declarados

INSERT INTO personal_registrado (empresa_id, cuit, nombre_completo, fecha_ingreso, declarado, fecha_declaracion)
SELECT e.empresa_id, e.cuit, e.nombre, (e.fecha_registro::date), true, (e.fecha_declaracion::date)
FROM empleados e
WHERE e.declarado = true
  AND NOT EXISTS (
    SELECT 1 FROM personal_registrado pr
    WHERE pr.empresa_id = e.empresa_id AND pr.cuit = e.cuit
  );

