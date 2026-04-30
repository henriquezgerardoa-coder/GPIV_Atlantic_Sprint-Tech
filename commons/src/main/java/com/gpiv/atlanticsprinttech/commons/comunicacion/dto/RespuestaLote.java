package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaLote(
    Long id,
    String codigo,
    Double superficieMetrosCuadrados,
    boolean ocupado,
    Long empresaId,
    String nombreEmpresa
) {
}

