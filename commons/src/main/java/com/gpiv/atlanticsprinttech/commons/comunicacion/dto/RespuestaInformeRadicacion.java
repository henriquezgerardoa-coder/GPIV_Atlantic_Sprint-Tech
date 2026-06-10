package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDate;

public record RespuestaInformeRadicacion(
    Long id,
    String numeroRadicado,
    String nombreEmpresa,
    String cuitEmpresa,
    String actividadEconomica,
    String estado,
    LocalDate fechaRadicacion,
    LocalDate fechaPlazo,
    String codigoLote,
    Integer tiempoEstimadoObraMeses,
    Integer empleadosPrevistos,
    // Etapa 1 — Empleabilidad / Materia Prima / Gestión Ambiental
    Integer etapa1Criterio1,
    Integer etapa1Criterio2,
    Integer etapa1Criterio3,
    Integer etapa1Puntuacion,
    String etapa1Observaciones,
    boolean etapa1Completa,
    // Etapa 2 — Financiero / Rentabilidad
    Integer etapa2Criterio1,
    Integer etapa2Criterio2,
    Integer etapa2Criterio3,
    Integer etapa2Puntuacion,
    String etapa2Observaciones,
    boolean etapa2Completa,
    // Etapa 3 — Preadjudicación / Cronograma de Obra
    Integer etapa3Criterio1,
    Integer etapa3Criterio2,
    Integer etapa3Criterio3,
    Integer etapa3Puntuacion,
    String etapa3Observaciones,
    boolean etapa3Completa,
    Integer puntuacionTotal,
    String evaluador
) {}