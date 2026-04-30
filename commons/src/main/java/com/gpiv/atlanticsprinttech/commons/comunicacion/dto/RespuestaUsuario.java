package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import java.util.Set;

public record RespuestaUsuario(
    Long id,
    String nombreUsuario,
    String nombreCompleto,
    boolean activo,
    Set<RolUsuario> roles
) {
}

