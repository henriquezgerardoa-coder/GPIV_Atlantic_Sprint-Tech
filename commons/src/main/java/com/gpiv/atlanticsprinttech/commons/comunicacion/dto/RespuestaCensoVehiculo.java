package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaCensoVehiculo(
    Long id,
    String patente,
    String marcaModelo,
    String tipo,
    boolean patenteValida
) {
}