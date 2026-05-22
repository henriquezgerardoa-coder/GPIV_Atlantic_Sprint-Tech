package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaCenso(
    Long id,
    int anioPeriodo,
    String fechaDeclaracion,
    int cantidadPersonalRegistrado,
    int cantidadPersonasNoRegistrado,
    int totalEmpleados,
    String observacion
) {
}
