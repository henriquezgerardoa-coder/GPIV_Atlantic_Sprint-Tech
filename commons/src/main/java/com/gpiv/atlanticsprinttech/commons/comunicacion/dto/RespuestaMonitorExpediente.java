package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaMonitorExpediente(
    Long id,
    String numeroRadicado,
    String nombreEmpresa,
    Long empresaId,
    String tipoSolicitud,
    String estado,
    String fechaRadicacion,
    String fechaUltimaActualizacion,
    String fechaAprobacion,
    String fechaPlazo,
    long diasSinMovimiento,
    boolean plazoVencido
) {}
