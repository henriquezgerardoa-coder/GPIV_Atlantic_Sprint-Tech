package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaCenso(
    Long id,
    int anioPeriodo,
    String fechaDeclaracion,
    int cantidadPersonalRegistrado,
    int cantidadPersonalNoRegistrado,
    int totalEmpleados,
    String observacion
) {
}
