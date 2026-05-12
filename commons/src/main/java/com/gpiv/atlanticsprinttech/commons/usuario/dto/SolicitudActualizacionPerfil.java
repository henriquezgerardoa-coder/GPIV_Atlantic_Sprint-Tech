package com.gpiv.atlanticsprinttech.commons.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudActualizacionPerfil(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres")
    String nombreCompleto,
    @Email(message = "El correo electronico no tiene un formato valido")
    @Size(max = 160, message = "El correo electronico no puede superar 160 caracteres")
    String correoElectronico
) {
}

