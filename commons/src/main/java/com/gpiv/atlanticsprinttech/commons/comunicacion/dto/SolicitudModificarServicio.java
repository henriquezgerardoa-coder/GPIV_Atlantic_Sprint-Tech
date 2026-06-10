package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudModificarServicio(
    @NotBlank @Size(max = 120) String nombre,
    @Size(max = 1000) String descripcionTecnica
) {}
