package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RespuestaEmpresaDetalleAdmin(
    Long id,
    String nombre,
    String razonSocial,
    String cuit,
    String telefono,
    String direccion,
    LocalDateTime fechaRegistro,
    String statusEmpresa,
    String actividadEconomica,
    String correoElectronico,
    Integer totalEmpleados,
    Integer totalVehiculos,
    String estadoExpediente,
    Long rubroId,
    String rubroNombre,
    String referente,
    String ingresosBrutos,
    Integer cantidadEmpleados,
    List<RespuestaUsuarioEmpresaAdmin> usuariosAsociados,
    List<RespuestaVehiculoEmpresa> vehiculos,
    List<LoteResumen> lotes,
    ServiciosResumen serviciosPostRadicacion
) {
    public record LoteResumen(
        Long id,
        String codigo,
        String zona,
        Double superficieMetrosCuadrados,
        String estadoAsignacion,
        LocalDate fechaAsignacion,
        String numeroExpedienteReferencia
    ) {}

    public record ServiciosResumen(
        Boolean solicitaAguaCruda,
        Double consumoAguaCrudaM3,
        Double consumoLuzKwh,
        Double consumoGasM3,
        Double consumoInternetMbps,
        List<RespuestaConsumoServicioPostRadicacion> consumosAdicionales
    ) {}
}