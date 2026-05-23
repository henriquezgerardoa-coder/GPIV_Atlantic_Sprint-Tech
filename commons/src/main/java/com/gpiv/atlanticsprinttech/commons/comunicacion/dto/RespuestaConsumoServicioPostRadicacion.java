package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaConsumoServicioPostRadicacion(
    String nombre,
    Double consumoEstimado,
    String unidad,
    String detalle
) {
}

