package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudAsignacionLote(
    @NotNull(message = "El ID de lote es obligatorio")
    Long loteId
) {}