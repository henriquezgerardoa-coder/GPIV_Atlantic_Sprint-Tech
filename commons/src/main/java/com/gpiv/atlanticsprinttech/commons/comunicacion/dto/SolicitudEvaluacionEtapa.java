package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudEvaluacionEtapa(
    @NotNull(message = "El criterio 1 es obligatorio")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    Integer criterio1,

    @NotNull(message = "El criterio 2 es obligatorio")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    Integer criterio2,

    @NotNull(message = "El criterio 3 es obligatorio")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    Integer criterio3,

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    String observaciones
) {}