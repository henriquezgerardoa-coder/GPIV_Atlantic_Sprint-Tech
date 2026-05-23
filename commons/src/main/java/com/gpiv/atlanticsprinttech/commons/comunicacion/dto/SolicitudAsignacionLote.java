package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudAsignacionLote(
    @NotNull(message = "Debe indicar el loteId")
    Long loteId
) {
}