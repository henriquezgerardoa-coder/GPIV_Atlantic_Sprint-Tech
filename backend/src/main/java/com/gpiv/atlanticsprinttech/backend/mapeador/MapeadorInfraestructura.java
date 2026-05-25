package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServicio;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServicioEvento;
import com.gpiv.atlanticsprinttech.entities.dominio.Servicio;
import com.gpiv.atlanticsprinttech.entities.dominio.ServicioEvento;
import org.springframework.stereotype.Component;

/**
 * Convierte las entidades de infraestructura {@link Servicio} y {@link ServicioEvento}
 * a sus DTOs de respuesta.
 */
@Component
public class MapeadorInfraestructura {

    public RespuestaServicio aRespuesta(Servicio servicio) {
        return new RespuestaServicio(
            servicio.getId(),
            servicio.getNombre(),
            servicio.getDescripcionTecnica(),
            servicio.getEstadoActual().name(),
            servicio.getUltimoComentario(),
            servicio.getUltimoTecnicoResponsable() != null
                ? servicio.getUltimoTecnicoResponsable().getNombreCompleto()
                : null,
            servicio.getFechaUltimaActualizacion() != null
                ? servicio.getFechaUltimaActualizacion().toString()
                : null
        );
    }

    public RespuestaServicioEvento aRespuestaEvento(ServicioEvento evento) {
        return new RespuestaServicioEvento(
            evento.getId(),
            evento.getEstado().name(),
            evento.getComentario(),
            evento.getUsuario(),
            evento.getFechaEvento() != null ? evento.getFechaEvento().toString() : null
        );
    }
}

