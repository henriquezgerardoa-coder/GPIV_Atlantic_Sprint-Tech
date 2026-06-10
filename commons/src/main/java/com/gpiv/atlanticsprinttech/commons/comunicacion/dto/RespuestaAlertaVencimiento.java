package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaAlertaVencimiento(
    Long loteId,
    String codigoLote,
    String nombreEmpresa,
    String etapa,
    String etapaEtiqueta,
    String fechaLimiteEtapaActual,
    long diasVencida
) {
}
