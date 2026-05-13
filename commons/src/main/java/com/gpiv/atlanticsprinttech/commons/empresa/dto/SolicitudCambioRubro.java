package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudCambioRubro(
    @NotNull(message = "El ID del nuevo rubro es obligatorio")
    Long idNuevoRubro,

    @NotBlank(message = "La justificacion es obligatoria")
    String justificacion
) { }
