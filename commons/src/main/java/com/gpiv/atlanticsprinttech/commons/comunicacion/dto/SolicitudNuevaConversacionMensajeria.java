package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudNuevaConversacionMensajeria(
    Long usuarioResponsableId,
    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 160, message = "El asunto no puede superar 160 caracteres")
    String asunto,
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    String mensaje
) {
}

