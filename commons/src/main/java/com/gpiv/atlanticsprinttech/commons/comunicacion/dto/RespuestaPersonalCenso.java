package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaPersonalCenso(
    Long id,
    String cuit,
    String nombreCompleto,
    String fechaIngreso,
    boolean declarado
) {
}
