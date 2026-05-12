package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RespuestaProyecto(
    Long id,
    String nombre,
    EstadoProyecto estado,
    String descripcion,
    LocalDate fechaInicioReal,
    LocalDate fechaEstimadaFin,
    BigDecimal montoInversion,
    LocalDateTime fechaCreacion,
    Long empresaId,
    String nombreEmpresa,
    Long loteId,
    String codigoLote,
    String responsableSeguimiento,
    Long radicacionOrigenId,
    double avanceFisico,
    boolean plazoVencido
) {
}