package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudActualizarServicio(
    @NotBlank String estado,
    @Size(max = 500) String comentario
) {}
