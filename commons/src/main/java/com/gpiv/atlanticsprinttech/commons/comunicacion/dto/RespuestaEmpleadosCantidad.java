package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

public record RespuestaEmpleadosCantidad(
	Long empresaId,
	String nombreEmpresa,
	long cantidadEmpleados
) {
}

