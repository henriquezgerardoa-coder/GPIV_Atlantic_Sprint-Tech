package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RespuestaEmpresaDetalleAdmin(
    Long id,
    String nombre,
    String razonSocial,
    String nit,
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
    RespuestaUsuarioEmpresaAdmin usuarioAsociado,
    List<RespuestaVehiculoEmpresa> vehiculos
) {
}

