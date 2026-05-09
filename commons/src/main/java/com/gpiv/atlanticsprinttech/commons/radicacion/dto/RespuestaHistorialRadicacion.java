package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import java.time.LocalDateTime;

public record RespuestaHistorialRadicacion(
    Long id,
    EstadoRadicacion estadoAnterior,
    EstadoRadicacion estado,
    String comentario,
    String usuario,
    LocalDateTime fechaEvento
) {
}
