package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCenso(
    @NotNull(message = "El año de período es obligatorio")
    @Min(value = 2000, message = "El año debe ser 2000 o posterior")
    @Max(value = 2100, message = "El año no es válido")
    Integer anioPeriodo,

    @Min(value = 0, message = "La cantidad de personal registrado no puede ser negativa")
    int cantidadPersonalRegistrado,

    @Min(value = 0, message = "La cantidad de personal no registrado no puede ser negativa")
    int cantidadPersonalNoRegistrado,

    @Size(max = 500, message = "La observación no puede superar 500 caracteres")
    String observacion
) {
}
