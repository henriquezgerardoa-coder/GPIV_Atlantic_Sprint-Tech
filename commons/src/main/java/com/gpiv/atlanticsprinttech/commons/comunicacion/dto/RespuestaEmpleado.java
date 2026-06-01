package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.time.LocalDateTime;

public record RespuestaEmpleado(
	Long id,
	String cuit,
	String nombre,
	LocalDateTime fechaRegistro,
	boolean declarado
) {
}

