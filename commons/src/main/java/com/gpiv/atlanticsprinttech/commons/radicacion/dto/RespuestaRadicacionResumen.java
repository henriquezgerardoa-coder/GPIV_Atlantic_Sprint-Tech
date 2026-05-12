package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RespuestaRadicacionResumen(
    Long id,
    String numeroRadicado,
    String tipoSolicitud,
    String estado,
    LocalDate fechaRadicacion,
    LocalDateTime fechaUltimaActualizacion,
    Integer necesidadMetrosCuadrados
) {}