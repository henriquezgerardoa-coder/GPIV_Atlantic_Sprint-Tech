package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaResumenDeclaracion(
    long declarados,
    long pendientes,
    String ultimaDeclaracion
) {
}
