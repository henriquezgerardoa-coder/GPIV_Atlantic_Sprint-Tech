package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SolicitudProyecto(
    @NotBlank @Size(max = 160) String nombre,
    @Size(max = 1000) String descripcion,
    BigDecimal montoInversion,
    String fechaEstimadaFin,
    Long radicacionId,
    Long responsableId
) {}
