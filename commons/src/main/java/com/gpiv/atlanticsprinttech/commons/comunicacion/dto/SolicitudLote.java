package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudLote(
    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 40, message = "El codigo no puede superar 40 caracteres")
    String codigo,

    @NotNull(message = "La superficie es obligatoria")
    @DecimalMin(value = "0.01", message = "La superficie debe ser mayor a 0")
    Double superficieMetrosCuadrados,

    boolean ocupado,

    Long empresaId,

    @Size(max = 40, message = "El estado de asignacion no puede superar 40 caracteres")
    String estadoAsignacion,

    @Size(max = 40, message = "El numero de expediente no puede superar 40 caracteres")
    String numeroExpedienteReferencia
) {
}

