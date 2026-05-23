package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRespuestaConversacionMensajeria(
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    String mensaje
) {
}

