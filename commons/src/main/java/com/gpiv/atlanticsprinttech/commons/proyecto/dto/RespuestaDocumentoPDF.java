package com.gpiv.atlanticsprinttech.commons.proyecto.dto;

import java.time.LocalDateTime;

public record RespuestaDocumentoPDF(
    Long id,
    String nombreArchivo,
    LocalDateTime fechaCarga,
    long tamanoBytes
) {
}