package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCensoVehiculo(
    @NotBlank(message = "La patente es obligatoria")
    @Size(max = 15, message = "La patente no puede superar 15 caracteres")
    String patente,

    @NotBlank(message = "La marca/modelo es obligatoria")
    @Size(max = 120, message = "La marca/modelo no puede superar 120 caracteres")
    String marcaModelo
) {
}
