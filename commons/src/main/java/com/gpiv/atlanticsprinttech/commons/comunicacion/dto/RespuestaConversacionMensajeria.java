package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaConversacionMensajeria(
    Long id,
    String tipoOrigen,
    Boolean consultaPublica,
    Long empresaId,
    String empresaNombre,
    String contactoNombreEmpresa,
    String contactoCorreoElectronico,
    String contactoTelefono,
    Long usuarioResponsableId,
    String usuarioResponsableNombre,
    String usuarioResponsableNombreCompleto,
    Long usuarioIniciadorId,
    String usuarioIniciadorNombre,
    String usuarioIniciadorNombreCompleto,
    String asunto,
    String fechaCreacion,
    String fechaUltimaActualizacion,
    String ultimoMensaje,
    Integer totalMensajes,
    List<RespuestaMensajeMensajeria> mensajes,
    String bandeja
) {
}

