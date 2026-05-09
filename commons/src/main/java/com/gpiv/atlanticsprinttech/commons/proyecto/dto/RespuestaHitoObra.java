package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import java.time.LocalDate;

public record RespuestaHitoObra(
    Long id,
    String descripcion,
    LocalDate fechaVencimientoReal,
    boolean cumplido,
    boolean vencido
) {
}