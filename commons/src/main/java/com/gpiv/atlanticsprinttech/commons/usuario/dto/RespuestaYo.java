package com.gpiv.atlanticsprinttech.commons.usuario.dto;

import java.util.List;

public record RespuestaYo(
	String nombreUsuario,
	String nombreCompleto,
	String correoElectronico,
	List<String> roles,
	Long empresaId,
	String nombreEmpresa
) {}

