package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudSolicitarCambioRubro(
    @NotNull(message = "Debe seleccionar el rubro solicitado")
    Long rubroSolicitadoId,

    @NotBlank(message = "La justificación es obligatoria")
    @Size(max = 1000, message = "La justificación no puede superar 1000 caracteres")
    String justificacion
) {
}
