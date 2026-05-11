package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import com.gpiv.atlanticsprinttech.entities.usuario.RolUsuario;
import java.time.LocalDateTime;
import java.util.Set;

public record RespuestaUsuarioEmpresaAdmin(
    String nombreUsuario,
    String nombreCompleto,
    String correoElectronico,
    Set<RolUsuario> roles,
    Boolean activo,
    LocalDateTime fechaUltimoAcceso
) {
}

