package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaHitoObra(
    Long id,
    String descripcion,
    String fechaVencimiento,
    boolean cumplido,
    boolean vencido
) {}
