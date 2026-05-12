package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudVincularEmpresa(
    @NotNull(message = "El ID de empresa es obligatorio")
    Long empresaId
) {}