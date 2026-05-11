package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudIniciarProyecto(
    @NotNull(message = "El id de la radicacion de origen es obligatorio")
    Long radicacionId
) {
}