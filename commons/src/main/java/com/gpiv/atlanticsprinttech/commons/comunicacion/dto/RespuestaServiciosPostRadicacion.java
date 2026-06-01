package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaServiciosPostRadicacion(
    Long empresaId,
    Integer cantidadEmpleados,
    List<RespuestaVehiculoEmpresa> vehiculos,
    Boolean solicitaAguaCruda,
    Double consumoAguaCrudaM3,
    Double consumoLuzKwh,
    Double consumoGasM3,
    Double consumoInternetMbps,
    List<RespuestaConsumoServicioPostRadicacion> consumosAdicionales,
    String zonaLote
) {
}
