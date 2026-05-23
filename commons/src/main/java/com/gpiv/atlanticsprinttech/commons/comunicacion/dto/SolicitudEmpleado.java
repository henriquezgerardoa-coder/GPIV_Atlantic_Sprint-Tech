package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudEmpleado(
	@NotBlank(message = "El CUIT es obligatorio")
	@Pattern(regexp = "^[0-9]{11}$", message = "El CUIT debe contener 11 dígitos")
	String cuit,

	@NotBlank(message = "El nombre del empleado es obligatorio")
	@Size(min = 1, max = 120, message = "El nombre debe tener entre 1 y 120 caracteres")
	String nombre
) {
}

