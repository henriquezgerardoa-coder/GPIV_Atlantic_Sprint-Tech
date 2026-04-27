package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRegistroPublico(
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "Debe ingresar un correo electronico valido")
    @Size(max = 160, message = "El correo electronico no puede superar 160 caracteres")
    String correoElectronico,
    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "La contrasena debe incluir mayusculas, minusculas y numeros"
    )
    String clave,
    @NotBlank(message = "La confirmacion de contrasena es obligatoria")
    String confirmacionClave
) {
}

