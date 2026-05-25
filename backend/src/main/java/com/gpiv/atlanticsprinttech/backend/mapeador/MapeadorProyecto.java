package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHitoObra;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import org.springframework.stereotype.Component;

/**
 * Convierte las entidades {@link ProyectoProductivo} y {@link HitoObra} a sus DTOs de respuesta.
 */
@Component
public class MapeadorProyecto {

    public RespuestaProyecto aRespuesta(ProyectoProductivo proyecto) {
        RadicacionSolicitud solicitud = proyecto.getSolicitudOrigen();
        return new RespuestaProyecto(
            proyecto.getId(),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            proyecto.getEstado().name(),
            proyecto.getFechaInicioReal() != null ? proyecto.getFechaInicioReal().toString() : null,
            proyecto.getFechaEstimadaFin() != null ? proyecto.getFechaEstimadaFin().toString() : null,
            proyecto.getMontoInversion(),
            proyecto.getFechaCreacion() != null ? proyecto.getFechaCreacion().toString() : null,
            solicitud != null ? solicitud.getId() : null,
            solicitud != null ? solicitud.getEmpresa().getNombre() : null,
            proyecto.getResponsableSeguimiento() != null
                ? proyecto.getResponsableSeguimiento().getNombreCompleto()
                : null,
            proyecto.calcularAvanceFisico(),
            proyecto.validarVencimientoPlazo(),
            proyecto.getHitos().stream()
                .map(this::aRespuestaHito)
                .toList()
        );
    }

    public RespuestaHitoObra aRespuestaHito(HitoObra hito) {
        return new RespuestaHitoObra(
            hito.getId(),
            hito.getDescripcion(),
            hito.getFechaVencimientoReal() != null ? hito.getFechaVencimientoReal().toString() : null,
            hito.isCumplido(),
            hito.estaVencido()
        );
    }
}

