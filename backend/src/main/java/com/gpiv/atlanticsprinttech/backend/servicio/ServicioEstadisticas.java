package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;

/**
 * Servicio de informes y estadisticas del parque industrial.
 * Accesible para ADMINISTRADOR y DIRECTIVO (R-14).
 */
public interface ServicioEstadisticas {

    /** Resumen general consolidado. */
    RespuestaEstadisticas obtenerResumen();
}

