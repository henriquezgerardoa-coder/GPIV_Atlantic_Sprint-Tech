package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import com.gpiv.atlanticsprinttech.commons.radicacion.dto.RespuestaRadicacionResumen;
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
    String nombreRubro,
    String correoElectronico,
    Integer totalEmpleados,
    Integer totalVehiculos,
    String estadoExpediente,
    List<RespuestaUsuarioEmpresaAdmin> usuariosAsociados,
    List<RespuestaVehiculoEmpresa> vehiculos,
    List<RespuestaRadicacionResumen> radicaciones
) {
}

