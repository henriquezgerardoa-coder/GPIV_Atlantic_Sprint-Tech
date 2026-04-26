package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudEmpresa(
	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 120, message = "El nombre no puede superar 120 caracteres")
	String nombre,
	@NotBlank(message = "El CUIT es obligatorio")
	@Size(max = 20, message = "El CUIT no puede superar 20 caracteres")
	String cuit,
	@NotBlank(message = "El correo electronico es obligatorio")
	@Email(message = "El correo electronico no es valido")
	@Size(max = 120, message = "El correo electronico no puede superar 120 caracteres")
	String correoElectronico
) {
}

