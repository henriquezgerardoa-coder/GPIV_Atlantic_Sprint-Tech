package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDateTime;

public record RespuestaEmpresa(
	Long id,
	String nombre,
	String razonSocial,
	String cuit,
	String direccion,
	String actividadEconomica,
	String correoElectronico,
	String telefono,
	Integer cantidadEmpleados,
	boolean permiteServiciosPostRadicacion,
	Long rubroId,
	String rubroNombre,
	LocalDateTime fechaRegistro,
	String statusEmpresa,
	String referente,
	String ingresosBrutos
) {
}

