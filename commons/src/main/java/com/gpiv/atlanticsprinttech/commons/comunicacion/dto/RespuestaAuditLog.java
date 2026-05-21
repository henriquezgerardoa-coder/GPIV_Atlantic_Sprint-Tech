package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDateTime;

public record RespuestaAuditLog(
    Long id,
    LocalDateTime fechaHora,
    String usuarioResponsable,
    String operacion,
    String entidadAfectada,
    String idReferencia,
    String valorAnterior,
    String valorNuevo,
    String direccionIp
) {
}