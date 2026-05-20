package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RespuestaInformeEmpresa(
    Long id,
    String nombre,
    String cuit,
    String razonSocial,
    String actividadEconomica,
    String correoElectronico,
    String telefono,
    String direccion,
    Integer cantidadEmpleados,
    LocalDateTime fechaRegistro,
    LocalDate fechaUltimaRadicacion,
    String estadoUltimaRadicacion,
    String numeroUltimaRadicacion,
    List<LoteInforme> lotes
) {
    public record LoteInforme(
        Long id,
        String codigo,
        Double superficieMetrosCuadrados,
        String estadoAsignacion,
        String zona,
        LocalDate fechaAsignacion
    ) {}
}
