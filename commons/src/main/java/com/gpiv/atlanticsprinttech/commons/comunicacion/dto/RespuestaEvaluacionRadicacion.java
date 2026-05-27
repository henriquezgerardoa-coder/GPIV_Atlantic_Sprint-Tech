package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDateTime;

public record RespuestaEvaluacionRadicacion(
    Long id,
    Long radicacionId,

    // Etapa 1 — Empleabilidad / Materia Prima / Reciclaje
    Integer etapa1Criterio1,
    Integer etapa1Criterio2,
    Integer etapa1Criterio3,
    String etapa1Observaciones,
    boolean etapa1Completa,
    Integer etapa1Puntuacion,

    // Etapa 2 — Financiero / Rentabilidad
    Integer etapa2Criterio1,
    Integer etapa2Criterio2,
    Integer etapa2Criterio3,
    String etapa2Observaciones,
    boolean etapa2Completa,
    Integer etapa2Puntuacion,

    // Etapa 3 — Preadjudicación / Cronograma de Obra
    Integer etapa3Criterio1,
    Integer etapa3Criterio2,
    Integer etapa3Criterio3,
    String etapa3Observaciones,
    boolean etapa3Completa,
    Integer etapa3Puntuacion,

    Integer puntuacionTotal,
    String evaluador,
    LocalDateTime fechaActualizacion
) {}