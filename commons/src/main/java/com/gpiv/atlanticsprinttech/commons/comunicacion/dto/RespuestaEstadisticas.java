package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.Map;

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
    long radicacionesRechazadas,
    long lotesProyectoEnEvaluacion,
    long lotesAdjudicadoPrecario,
    long lotesEnConstruccion,
    long lotesOperativos,
    long lotesEscriturados,
    long lotesRevertidos
) {}
