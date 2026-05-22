package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioInfraestructura;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServicio;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServicioEvento;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudActualizarServicio;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCrearServicio;
import com.gpiv.atlanticsprinttech.entities.dominio.Servicio;
import com.gpiv.atlanticsprinttech.entities.dominio.ServicioEvento;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/infraestructura")
public class ControladorInfraestructura {

    private final ServicioInfraestructura servicio;

    public ControladorInfraestructura(ServicioInfraestructura servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaServicio> listar() {
        return servicio.listar().stream().map(this::toRespuesta).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaServicio crear(@Valid @RequestBody SolicitudCrearServicio solicitud, Authentication auth) {
        return toRespuesta(servicio.crear(auth.getName(), solicitud.nombre(), solicitud.descripcionTecnica()));
    }

    @PatchMapping("/{id}/estado")
    public RespuestaServicio actualizarEstado(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudActualizarServicio solicitud,
        Authentication auth
    ) {
        return toRespuesta(servicio.actualizarEstado(auth.getName(), id, solicitud.estado(), solicitud.comentario()));
    }

    @GetMapping("/{id}/historial")
    public List<RespuestaServicioEvento> historial(@PathVariable Long id) {
        return servicio.listarHistorial(id).stream().map(this::toEvento).toList();
    }

    private RespuestaServicio toRespuesta(Servicio s) {
        return new RespuestaServicio(
            s.getId(),
            s.getNombre(),
            s.getDescripcionTecnica(),
            s.getEstadoActual().name(),
            s.getUltimoComentario(),
            s.getUltimoTecnicoResponsable() != null ? s.getUltimoTecnicoResponsable().getNombreCompleto() : null,
            s.getFechaUltimaActualizacion() != null ? s.getFechaUltimaActualizacion().toString() : null
        );
    }

    private RespuestaServicioEvento toEvento(ServicioEvento e) {
        return new RespuestaServicioEvento(
            e.getId(),
            e.getEstado().name(),
            e.getComentario(),
            e.getUsuario(),
            e.getFechaEvento() != null ? e.getFechaEvento().toString() : null
        );
    }
}
