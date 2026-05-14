package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SolicitudActualizacionUsuario(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres")
    String nombreCompleto,
    boolean activo,
    @NotEmpty(message = "Debe asignar al menos un rol")
    Set<RolUsuario> roles,
    Long empresaId
) {
}

