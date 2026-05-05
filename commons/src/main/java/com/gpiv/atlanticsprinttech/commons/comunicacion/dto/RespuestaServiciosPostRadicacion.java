package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaServiciosPostRadicacion(
    Long empresaId,
    Integer cantidadEmpleados,
    List<RespuestaVehiculoEmpresa> vehiculos
) {
}

