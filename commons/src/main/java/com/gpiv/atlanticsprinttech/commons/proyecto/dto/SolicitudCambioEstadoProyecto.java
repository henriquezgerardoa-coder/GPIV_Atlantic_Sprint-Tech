package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import jakarta.validation.constraints.NotNull;

public record SolicitudCambioEstadoProyecto(
    @NotNull(message = "El estado es obligatorio")
    EstadoProyecto estado
) {
}