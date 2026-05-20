package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDate;

public record RespuestaInformeLote(
    Long id,
    String codigo,
    Double superficieMetrosCuadrados,
    boolean ocupado,
    String estadoAsignacion,
    String zona,
    String nombreEmpresa,
    String cuitEmpresa,
    LocalDate fechaAsignacion,
    String numeroExpedienteReferencia,
    LocalDate fechaPlazoRadicacion
) {}