package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.math.BigDecimal;
import java.util.List;

public record RespuestaProyecto(
    Long id,
    String nombre,
    String descripcion,
    String estado,
    String fechaInicioReal,
    String fechaEstimadaFin,
    BigDecimal montoInversion,
    String fechaCreacion,
    Long radicacionId,
    Long loteId,
    String codigoLote,
    String nombreEmpresa,
    String responsableSeguimiento,
    double avanceFisico,
    boolean tieneHitosVencidos,
    List<RespuestaHitoObra> hitos
) {}
