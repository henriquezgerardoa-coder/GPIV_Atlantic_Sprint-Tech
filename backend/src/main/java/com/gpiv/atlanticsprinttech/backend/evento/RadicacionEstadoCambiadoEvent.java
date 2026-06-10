package com.gpiv.atlanticsprinttech.backend.evento;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;

/**
 * Publicado cada vez que una radicación cambia de estado.
 * Los listeners (notificaciones, auditoría externa, etc.) se suscriben
 * a este evento sin acoplar el servicio a cada canal de destino.
 */
public record RadicacionEstadoCambiadoEvent(
    RadicacionSolicitud radicacion,
    EstadoRadicacion estadoAnterior,
    EstadoRadicacion estadoNuevo,
    String comentario,
    String identificadorIngreso
) {}