package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SolicitudCambioEstadoRadicacion(
    @NotNull(message = "El estado es obligatorio")
    EstadoRadicacion estado,
    @Size(max = 1000, message = "El comentario no puede superar 1000 caracteres")
    String comentario,
    Integer tiempoEstimadoObraMeses,
    LocalDate fechaPlazo
) {
}
