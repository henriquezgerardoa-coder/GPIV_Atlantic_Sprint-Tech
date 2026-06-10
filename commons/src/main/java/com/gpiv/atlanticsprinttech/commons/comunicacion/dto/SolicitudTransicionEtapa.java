package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudTransicionEtapa(
    @NotBlank(message = "La etapa destino es obligatoria")
    @Size(max = 40)
    String etapa,

    @Size(max = 2000, message = "El motivo no puede superar 2000 caracteres")
    String motivo
) {
}
