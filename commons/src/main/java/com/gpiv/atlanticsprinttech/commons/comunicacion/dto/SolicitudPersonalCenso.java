package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudPersonalCenso(
    @NotBlank(message = "El CUIT es obligatorio")
    @Size(max = 20, message = "El CUIT no puede superar 20 caracteres")
    String cuit,

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres")
    String nombreCompleto,

    @NotNull(message = "La fecha de ingreso es obligatoria")
    String fechaIngreso
) {
}
