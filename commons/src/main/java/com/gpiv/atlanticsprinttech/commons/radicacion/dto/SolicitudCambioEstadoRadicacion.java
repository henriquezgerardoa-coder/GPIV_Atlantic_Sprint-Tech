package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCambioEstadoRadicacion(
    @NotNull(message = "El estado es obligatorio")
    EstadoRadicacion estado,
    @Size(max = 1000, message = "El comentario no puede superar 1000 caracteres")
    String comentario
) {
}
