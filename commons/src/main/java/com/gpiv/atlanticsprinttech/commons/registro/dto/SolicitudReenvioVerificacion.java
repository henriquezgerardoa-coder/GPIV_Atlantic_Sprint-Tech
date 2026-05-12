package com.gpiv.atlanticsprinttech.commons.registro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudReenvioVerificacion(
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "Debe ingresar un correo electronico valido")
    @Size(max = 160, message = "El correo electronico no puede superar 160 caracteres")
    String correoElectronico
) {
}

