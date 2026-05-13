package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import com.gpiv.atlanticsprinttech.entities.empresa.Rubro;

public record RespuestaEmpresa(
	Long id,
	String nombre,
	String razonSocial,
	String nit,
	String cuit,
	String direccion,
	String correoElectronico,
	String telefono,
	Integer cantidadEmpleados,
	boolean permiteServiciosPostRadicacion,
	Rubro rubro
) {
}

