package com.gpiv.atlanticsprinttech.commons.empresa.dto;

public record RespuestaEmpresa(
	Long id,
	String nombre,
	String razonSocial,
	String nit,
	String cuit,
	String direccion,
	String actividadEconomica,
	String correoElectronico,
	String telefono,
	Integer cantidadEmpleados,
	boolean permiteServiciosPostRadicacion
) {
}

