package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudHitoObra(
    @NotBlank @Size(max = 300) String descripcion,
    String fechaVencimiento
) {}