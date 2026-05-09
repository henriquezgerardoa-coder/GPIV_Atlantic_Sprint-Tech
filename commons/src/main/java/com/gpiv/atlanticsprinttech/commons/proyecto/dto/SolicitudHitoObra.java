package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SolicitudHitoObra(
    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    String descripcion,

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    LocalDate fechaVencimientoReal
) {
}