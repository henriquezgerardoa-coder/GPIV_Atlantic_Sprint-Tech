-- Eliminar rol EMPRESA incorrecto del usuario 'tecnico' si fue asignado por error
DELETE FROM usuarios_roles
WHERE usuario_id = (SELECT id FROM usuarios WHERE nombre_usuario = 'tecnico')
  AND rol = 'EMPRESA';

-- Asegurar que el usuario 'tecnico' tenga el rol TECNICO
INSERT INTO usuarios_roles (usuario_id, rol)
SELECT id, 'TECNICO'
FROM usuarios
WHERE nombre_usuario = 'tecnico'
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur
    WHERE ur.usuario_id = usuarios.id AND ur.rol = 'TECNICO'
  );