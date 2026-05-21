package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RespuestaRadicacion(
    Long id,
    String numeroRadicado,
    Long empresaId,
    String nombreEmpresa,
    String tipoSolicitud,
    String descripcion,
    String usoEstimativo,
    Boolean tieneRelevamientoPedidoLotes,
    Integer etapaActual,
    EstadoRadicacion estado,
    LocalDate fechaRadicacion,
    LocalDateTime fechaUltimaActualizacion,
    Integer tiempoEstimadoObraMeses,
    LocalDate fechaPlazo,
    LocalDate fechaAprobacion,
    Integer tiempoSolicitadoMeses
) {
}
