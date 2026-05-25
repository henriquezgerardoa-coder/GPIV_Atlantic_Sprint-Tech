package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorInfraestructura;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioInfraestructura;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServicio;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServicioEvento;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudActualizarServicio;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCrearServicio;
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
    private final MapeadorInfraestructura mapeador;

    public ControladorInfraestructura(ServicioInfraestructura servicio, MapeadorInfraestructura mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    public List<RespuestaServicio> listar() {
        return servicio.listar().stream().map(mapeador::aRespuesta).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaServicio crear(@Valid @RequestBody SolicitudCrearServicio solicitud, Authentication auth) {
        return mapeador.aRespuesta(servicio.crear(auth.getName(), solicitud.nombre(), solicitud.descripcionTecnica()));
    }

    @PatchMapping("/{id}/estado")
    public RespuestaServicio actualizarEstado(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudActualizarServicio solicitud,
        Authentication auth
    ) {
        return mapeador.aRespuesta(servicio.actualizarEstado(auth.getName(), id, solicitud.estado(), solicitud.comentario()));
    }

    @GetMapping("/{id}/historial")
    public List<RespuestaServicioEvento> historial(@PathVariable Long id) {
        return servicio.listarHistorial(id).stream().map(mapeador::aRespuestaEvento).toList();
    }
}
