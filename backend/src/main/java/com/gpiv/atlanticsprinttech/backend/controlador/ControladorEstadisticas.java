package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Informes y estadisticas generales del parque industrial.
 * Accesible para ADMINISTRADOR y OPERADOR (R-14: lectura de informes para rol legislativo/operador).
 * El rol EMPRESA NO tiene acceso a este recurso.
 */
@RestController
@RequestMapping("/api/estadisticas")
public class ControladorEstadisticas {

    private final ServicioEstadisticas servicioEstadisticas;

    public ControladorEstadisticas(ServicioEstadisticas servicioEstadisticas) {
        this.servicioEstadisticas = servicioEstadisticas;
    }

    /**
     * Resumen general consolidado del parque industrial.
     * GET /api/estadisticas/resumen
     */
    @GetMapping("/resumen")
    public RespuestaEstadisticas obtenerResumen() {
        return servicioEstadisticas.obtenerResumen();
    }
}

