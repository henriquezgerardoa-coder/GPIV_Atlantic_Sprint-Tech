package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaEmpresa(
	Long id,
	String nombre,
	String cuit,
	String correoElectronico
) {
}

