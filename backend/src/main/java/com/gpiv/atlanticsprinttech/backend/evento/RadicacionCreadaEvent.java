package com.gpiv.atlanticsprinttech.backend.evento;

import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;

public record RadicacionCreadaEvent(
    RadicacionSolicitud radicacion,
    String identificadorIngreso
) {}