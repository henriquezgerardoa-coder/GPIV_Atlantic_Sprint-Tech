package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudConsumoServicioPostRadicacion(
    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 120, message = "El nombre del servicio no puede superar 120 caracteres")
    String nombre,
    @DecimalMin(value = "0.0", inclusive = true, message = "El consumo estimado no puede ser negativo")
    Double consumoEstimado,
    @Size(max = 40, message = "La unidad no puede superar 40 caracteres")
    String unidad,
    @Size(max = 250, message = "El detalle no puede superar 250 caracteres")
    String detalle
) {
}

