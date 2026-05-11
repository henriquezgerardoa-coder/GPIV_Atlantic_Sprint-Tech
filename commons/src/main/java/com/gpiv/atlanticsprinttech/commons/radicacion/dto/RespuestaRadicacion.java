package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
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
    Integer necesidadMetrosCuadrados,
    Integer etapaActual,
    EstadoRadicacion estado,
    LocalDate fechaRadicacion,
    LocalDateTime fechaUltimaActualizacion
) {
}
