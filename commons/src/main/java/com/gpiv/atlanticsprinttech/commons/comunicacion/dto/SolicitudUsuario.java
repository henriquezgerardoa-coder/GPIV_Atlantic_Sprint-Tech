package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SolicitudUsuario(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 60, message = "El nombre de usuario no puede superar 60 caracteres")
    String nombreUsuario,
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres")
    String nombreCompleto,
    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
    String clave,
    boolean activo,
    @NotEmpty(message = "Debe asignar al menos un rol")
    Set<RolUsuario> roles
) {
}

