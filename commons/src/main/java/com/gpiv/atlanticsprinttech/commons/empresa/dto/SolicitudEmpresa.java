package com.gpiv.atlanticsprinttech.commons.empresa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudEmpresa(
	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 120, message = "El nombre no puede superar 120 caracteres")
	String nombre,
	@NotBlank(message = "La razon social es obligatoria")
	@Size(max = 160, message = "La razon social no puede superar 160 caracteres")
	String razonSocial,
	@NotBlank(message = "El NIT es obligatorio")
	@Size(max = 30, message = "El NIT no puede superar 30 caracteres")
	String nit,
	@NotBlank(message = "El CUIT es obligatorio")
	@Size(max = 20, message = "El CUIT no puede superar 20 caracteres")
	String cuit,
	@NotBlank(message = "La direccion es obligatoria")
	@Size(max = 240, message = "La direccion no puede superar 240 caracteres")
	String direccion,
	@NotBlank(message = "La actividad economica es obligatoria")
	@Size(max = 180, message = "La actividad economica no puede superar 180 caracteres")
	String actividadEconomica,
	@NotBlank(message = "El correo electronico es obligatorio")
	@Email(message = "El correo electronico no es valido")
	@Size(max = 120, message = "El correo electronico no puede superar 120 caracteres")
	String correoElectronico,
	Integer cantidadEmpleados,
	@Size(max = 40, message = "El telefono no puede superar 40 caracteres")
	String telefono
) {
}

