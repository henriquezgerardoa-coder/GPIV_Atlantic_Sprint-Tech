package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaYo(
	String nombreUsuario,
	String nombreCompleto,
	String correoElectronico,
	List<String> roles,
	Long empresaId,
	String nombreEmpresa
) {}

