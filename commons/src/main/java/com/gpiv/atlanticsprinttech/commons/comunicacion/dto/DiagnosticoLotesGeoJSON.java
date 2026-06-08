package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

/**
 * DTO para sincronización: reporta el estado de completitud de geometrías en lotes.
 */
public record DiagnosticoLotesGeoJSON(
    long totalLotes,
    long lotesConGeometria,
    long lotesSinGeometria,
    double porcentajeCompletitud,
    long lotesConSubdivisiones,
    long lotesConParentePrincipal
) {
    public static DiagnosticoLotesGeoJSON crear(
        long total,
        long conGeom,
        long sinGeom,
        long conSub,
        long conPadre
    ) {
        double pct = total > 0 ? (conGeom * 100.0 / total) : 0;
        return new DiagnosticoLotesGeoJSON(total, conGeom, sinGeom, pct, conSub, conPadre);
    }
}

