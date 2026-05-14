package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SolicitudServiciosPostRadicacion(
    @NotNull(message = "La cantidad de empleados es obligatoria")
    @Min(value = 0, message = "La cantidad de empleados no puede ser negativa")
    Integer cantidadEmpleados,
    @Valid
    @Size(max = 100, message = "No se permiten mas de 100 vehiculos por empresa")
    List<SolicitudVehiculoEmpresa> vehiculos
) {
}

