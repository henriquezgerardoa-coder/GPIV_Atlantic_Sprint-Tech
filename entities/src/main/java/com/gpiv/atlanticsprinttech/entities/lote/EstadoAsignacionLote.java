package com.gpiv.atlanticsprinttech.entities.lote;

public enum EstadoAsignacionLote {
    DISPONIBLE,
    PREADJUDICADO_A_SOLICITUD,
    ASIGNADO_A_EMPRESA,
    ADJUDICADO_RADICADA // Añadido para resolver el error de la base de datos
}
