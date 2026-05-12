package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudObservacionRadicacion(
    @NotBlank(message = "La observacion es obligatoria")
    @Size(max = 1000, message = "La observacion no puede superar 1000 caracteres")
    String comentario
) {
}
