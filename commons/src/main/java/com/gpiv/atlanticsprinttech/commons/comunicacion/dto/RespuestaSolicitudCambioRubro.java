package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaSolicitudCambioRubro(
    Long id,
    Long empresaId,
    String nombreEmpresa,
    Long rubroSolicitadoId,
    String nombreRubroSolicitado,
    String rubroAnteriorNombre,
    String justificacion,
    String descripcionOtros,
    String estado,
    String motivoRechazo,
    Integer puntajeImpactoOperativo,
    Integer puntajeCompatibilidadParque,
    Integer puntajeViabilidadTecnica,
    Integer puntajeCumplimientoNormativo,
    String observacionesRubrica,
    String solicitadoPor,
    String fechaSolicitud,
    String resueltoPor,
    String fechaResolucion
) {
}
