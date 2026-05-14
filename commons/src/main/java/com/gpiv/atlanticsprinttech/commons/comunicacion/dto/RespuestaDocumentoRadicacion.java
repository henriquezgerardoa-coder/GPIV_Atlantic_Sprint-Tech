package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import com.gpiv.atlanticsprinttech.entities.dominio.TipoDocumentoRadicacion;
import java.time.LocalDateTime;

public record RespuestaDocumentoRadicacion(
    Long id,
    TipoDocumentoRadicacion tipoDocumento,
    String nombreArchivo,
    String mimeType,
    long tamanoBytes,
    String descripcion,
    String subidoPor,
    LocalDateTime fechaSubida
) {
}
