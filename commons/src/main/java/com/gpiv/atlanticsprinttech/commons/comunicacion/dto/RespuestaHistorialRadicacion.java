package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
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
