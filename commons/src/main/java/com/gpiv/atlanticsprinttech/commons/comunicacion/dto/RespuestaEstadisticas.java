package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.Map;

/**
 * Resumen estadistico del parque industrial.
 * Accesible para ADMINISTRADOR y OPERADOR (R-14: lectura de informes).
 */
public record RespuestaEstadisticas(
    long totalEmpresas,
    long totalLotes,
    long lotesOcupados,
    long lotesDisponibles,
    long totalRadicaciones,
    Map<String, Long> radicacionesPorEstado,
    long radicacionesPendientes,
    long radicacionesEnRevision,
    long radicacionesAprobadas,
    long radicacionesRechazadas
) {}

