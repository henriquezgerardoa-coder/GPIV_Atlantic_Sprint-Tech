package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaEmpresa(
	Long id,
	String nombre,
	String razonSocial, String cuit,
	String direccion,
	String actividadEconomica,
	String correoElectronico,
	String telefono,
	Integer cantidadEmpleados,
	boolean permiteServiciosPostRadicacion
) {
}

