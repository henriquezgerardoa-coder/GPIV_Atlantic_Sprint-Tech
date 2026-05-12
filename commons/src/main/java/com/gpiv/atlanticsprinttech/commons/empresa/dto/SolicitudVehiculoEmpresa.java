package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudVehiculoEmpresa(
    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 20, message = "La placa no puede superar 20 caracteres")
    String placa,
    @NotBlank(message = "El tipo de vehiculo es obligatorio")
    @Size(max = 40, message = "El tipo de vehiculo no puede superar 40 caracteres")
    String tipo,
    @Size(max = 200, message = "La descripcion no puede superar 200 caracteres")
    String descripcion
) {
}

