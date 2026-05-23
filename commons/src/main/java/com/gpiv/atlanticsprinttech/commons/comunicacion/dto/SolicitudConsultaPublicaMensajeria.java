package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudConsultaPublicaMensajeria(
    @NotBlank(message = "Debe ingresar su nombre o empresa")
    @Size(max = 160, message = "El nombre o empresa no puede superar 160 caracteres")
    String nombreOEmpresa,
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El correo electronico no es valido")
    @Size(max = 160, message = "El correo electronico no puede superar 160 caracteres")
    String correoElectronico,
    @Size(max = 40, message = "El telefono no puede superar 40 caracteres")
    String telefono,
    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 160, message = "El asunto no puede superar 160 caracteres")
    String asunto,
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    String mensaje
) {
}

