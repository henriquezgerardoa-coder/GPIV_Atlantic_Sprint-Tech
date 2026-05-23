package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
    List<SolicitudVehiculoEmpresa> vehiculos,
    Boolean solicitaAguaCruda,
    @DecimalMin(value = "0.0", inclusive = true, message = "El consumo estimado de agua cruda no puede ser negativo")
    Double consumoAguaCrudaM3,
    @DecimalMin(value = "0.0", inclusive = true, message = "El consumo estimado de luz no puede ser negativo")
    Double consumoLuzKwh,
    @DecimalMin(value = "0.0", inclusive = true, message = "El consumo estimado de gas no puede ser negativo")
    Double consumoGasM3,
    @DecimalMin(value = "0.0", inclusive = true, message = "El consumo estimado de internet no puede ser negativo")
    Double consumoInternetMbps,
    @Valid
    @Size(max = 30, message = "No se permiten mas de 30 servicios adicionales")
    List<SolicitudConsumoServicioPostRadicacion> consumosAdicionales
) {
}
