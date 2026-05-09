package com.gpiv.atlanticsprinttech.commons.usuario.dto;

import com.gpiv.atlanticsprinttech.entities.usuario.RolUsuario;
import java.util.Set;

public record RespuestaUsuario(
    Long id,
    String nombreUsuario,
    String nombreCompleto,
    boolean activo,
    Set<RolUsuario> roles,
    Long empresaId,
    String nombreEmpresa
) {
}

