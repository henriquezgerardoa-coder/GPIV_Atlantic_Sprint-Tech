package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRadicacion(
    @NotBlank(message = "El tipo de solicitud es obligatorio")
    @Size(max = 80, message = "El tipo de solicitud no puede superar 80 caracteres")
    String tipoSolicitud,
    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres")
    String descripcion
) {
}
