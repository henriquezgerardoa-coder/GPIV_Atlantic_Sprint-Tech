package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RespuestaRadicacion(
    Long id,
    String numeroRadicado,
    Long empresaId,
    String nombreEmpresa,
    String razonSocialEmpresa,
    String cuitEmpresa,
    String actividadEconomicaEmpresa,
    String direccionEmpresa,
    String correoElectronicoEmpresa,
    String telefonoEmpresa,
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
    Integer tiempoSolicitadoMeses,
    Long loteId,
    String codigoLote,
    Integer superficieSolicitadaM2,
    String numeroResolucion,
    String resueltoPor,
    Long proyectoId,
    String proyectoEstado,
    List<RespuestaEmpresaEnLoteCompartido> otrasEmpresasEnLote
) {
}
