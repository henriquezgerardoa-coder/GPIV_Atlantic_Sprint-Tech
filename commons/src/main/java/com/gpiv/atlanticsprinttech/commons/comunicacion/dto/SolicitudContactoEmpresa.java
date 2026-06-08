package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudContactoEmpresa(
    @NotBlank @Email @Size(max = 120) String correoElectronico,
    @Size(max = 40) String telefono
) {}