package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaServicioEvento(
    Long id,
    String estado,
    String comentario,
    String usuario,
    String fechaEvento
) {}
