package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaMensajeMensajeria(
    Long id,
    Long usuarioEmisorId,
    String usuarioEmisorNombre,
    String usuarioEmisorNombreCompleto,
    String emisorExternoNombre,
    Boolean emisorExterno,
    String contenido,
    String fechaEnvio
) {
}

