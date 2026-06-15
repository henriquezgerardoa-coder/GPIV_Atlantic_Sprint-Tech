package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaInformeServicio(
    Long id,
    String nombre,
    String estadoActual,
    String descripcionTecnica,
    List<EmpresaConsumidora> empresas
) {
    public record EmpresaConsumidora(
        Long empresaId,
        String nombreEmpresa,
        String cuitEmpresa,
        Long loteId,
        String codigoLote,
        String consumo
    ) {}
}