package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRestablecerClave(
    @NotBlank(message = "La clave nueva es obligatoria")
    @Size(min = 8, max = 72, message = "La clave nueva debe tener entre 8 y 72 caracteres")
    String claveNueva
) {
}

