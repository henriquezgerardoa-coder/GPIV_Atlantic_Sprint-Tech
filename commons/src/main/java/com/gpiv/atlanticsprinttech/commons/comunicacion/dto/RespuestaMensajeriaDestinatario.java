package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;

public record RespuestaMensajeriaDestinatario(
    Long id,
    String nombreUsuario,
    String nombreCompleto,
    List<String> roles
) {
}

