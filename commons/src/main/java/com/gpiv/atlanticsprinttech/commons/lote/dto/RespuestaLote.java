package com.gpiv.atlanticsprinttech.commons.lote.dto;

import org.locationtech.jts.geom.Polygon;

public record RespuestaLote(
    Long id,
    String codigo,
    Double superficieMetrosCuadrados,
    boolean ocupado,
    Long empresaId,
    String nombreEmpresa,
    String cuitEmpresa,
    String estadoAsignacion,
    String fechaAsignacion,
    String numeroExpedienteReferencia,
    Polygon poligono,
    String zona
) {
}

