package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaHistorialEtapa(
    Long id,
    Long loteId,
    String codigoLote,
    String etapaAnterior,
    String etapaAnteriorEtiqueta,
    String etapaNueva,
    String etapaNuevaEtiqueta,
    String fechaCambio,
    String usuarioResponsable,
    String motivo,
    String fechaLimitePlazo
) {
}
