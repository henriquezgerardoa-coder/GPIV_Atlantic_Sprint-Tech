package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaServicio(
    Long id,
    String nombre,
    String descripcionTecnica,
    String estadoActual,
    String ultimoComentario,
    String ultimoTecnicoNombre,
    String fechaUltimaActualizacion
) {}
