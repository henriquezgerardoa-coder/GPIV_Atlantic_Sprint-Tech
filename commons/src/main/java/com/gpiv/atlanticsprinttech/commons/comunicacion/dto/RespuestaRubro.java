package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaRubro(
    Long id,
    String nombre,
    String descripcion,
    boolean requierePermisoEspecial
) {
}
