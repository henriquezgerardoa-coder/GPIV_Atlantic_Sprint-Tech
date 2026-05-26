package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
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