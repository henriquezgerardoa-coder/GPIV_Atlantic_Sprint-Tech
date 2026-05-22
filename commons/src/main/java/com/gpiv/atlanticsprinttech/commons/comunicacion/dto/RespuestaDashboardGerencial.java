package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaDashboardGerencial(
    long totalEmpresas,
    long totalLotes,
    long lotesOcupados,
    long lotesLibres,
    double tasaOcupacion,
    double superficieTotalM2,
    double superficieOcupadaM2,
    long empleosActuales,
    long empleosProyectados,
    long radicacionesActivas,
    long proyectosActivos,
    long proyectosConHitosVencidos,
    List<ItemRubro> distribucionRubro,
    List<ItemZona> distribucionZona
) {
    public record ItemRubro(String nombre, long empresas) {}

    public record ItemZona(
        String zona,
        long total,
        long ocupados,
        long libres,
        double superficieM2,
        double tasaOcupacion
    ) {}
}
