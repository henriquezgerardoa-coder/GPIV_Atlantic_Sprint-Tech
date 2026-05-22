package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudResolverCambioRubro(
    @NotNull(message = "Debe indicar si se aprueba o rechaza")
    Boolean aprobada,

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    String motivoRechazo
) {
}
