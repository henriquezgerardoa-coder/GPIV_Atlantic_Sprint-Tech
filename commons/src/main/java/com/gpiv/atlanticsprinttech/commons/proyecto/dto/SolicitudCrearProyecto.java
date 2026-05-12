package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitudCrearProyecto(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    String nombre,

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 2000, message = "La descripcion no puede superar 2000 caracteres")
    String descripcion,

    @NotNull(message = "La fecha estimada de fin es obligatoria")
    LocalDate fechaEstimadaFin,

    @NotNull(message = "El monto de inversion es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto de inversion debe ser mayor a cero")
    BigDecimal montoInversion,

    @NotNull(message = "El lote es obligatorio")
    Long loteId,

    @NotNull(message = "El responsable de seguimiento es obligatorio")
    Long responsableSeguimientoId
) {
}