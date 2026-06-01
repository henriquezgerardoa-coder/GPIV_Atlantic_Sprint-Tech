package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDashboardGerencial;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeRadicacion;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Informes y estadisticas generales del parque industrial.
 * Accesible para ADMINISTRADOR y DIRECTIVO (R-14: lectura de informes para rol legislativo/directivo).
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

    /**
     * Informe por empresa con lotes asignados y estado del expediente.
     * GET /api/estadisticas/informes/empresas
     */
    @GetMapping("/informes/empresas")
    public List<RespuestaInformeEmpresa> informeEmpresas() {
        return servicioEstadisticas.obtenerInformeEmpresas();
    }

    /**
     * Informe por lotes con estado de asignación y detalles de empresa.
     * GET /api/estadisticas/informes/lotes
     */
    @GetMapping("/informes/lotes")
    public List<RespuestaInformeLote> informeLotes() {
        return servicioEstadisticas.obtenerInformeLotes();
    }

    /**
     * Informe por radicaciones con evaluación por etapas.
     * GET /api/estadisticas/informes/radicaciones
     */
    @GetMapping("/informes/radicaciones")
    public List<RespuestaInformeRadicacion> informeRadicaciones() {
        return servicioEstadisticas.obtenerInformeRadicaciones();
    }

    /**
     * Dashboard gerencial con KPIs, distribución por rubro y zona.
     * GET /api/estadisticas/gerencial
     */
    @GetMapping("/gerencial")
    public RespuestaDashboardGerencial dashboardGerencial() {
        return servicioEstadisticas.obtenerDashboardGerencial();
    }
}

