package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDashboardGerencial;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeRadicacion;
import java.util.List;

/**
 * Servicio de informes y estadisticas del parque industrial.
 * Accesible para ADMINISTRADOR, DIRECTIVO y SECRETARIO.
 */
public interface ServicioEstadisticas {

    /** Resumen general consolidado. */
    RespuestaEstadisticas obtenerResumen();

    /** Informe por empresa con lotes asignados y estado del expediente. */
    List<RespuestaInformeEmpresa> obtenerInformeEmpresas();

    /** Informe por lotes con estado de asignación y detalles. */
    List<RespuestaInformeLote> obtenerInformeLotes();

    /** Informe por radicaciones con datos de evaluación por etapas. */
    List<RespuestaInformeRadicacion> obtenerInformeRadicaciones();

    /** Dashboard gerencial con KPIs, distribución por rubro y zona. */
    RespuestaDashboardGerencial obtenerDashboardGerencial();
}

