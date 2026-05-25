package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorProyecto;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioProyecto;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHitoObra;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaProyecto;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudHitoObra;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudProyecto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos")
public class ControladorProyecto {

    private final ServicioProyecto servicio;
    private final MapeadorProyecto mapeador;

    public ControladorProyecto(ServicioProyecto servicio, MapeadorProyecto mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    public List<RespuestaProyecto> listar(Authentication auth) {
        return servicio.listar(auth.getName()).stream()
            .map(mapeador::aRespuesta)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaProyecto crear(@Valid @RequestBody SolicitudProyecto solicitud, Authentication auth) {
        LocalDate fechaFin = solicitud.fechaEstimadaFin() != null
            ? LocalDate.parse(solicitud.fechaEstimadaFin())
            : null;
        return mapeador.aRespuesta(servicio.crear(
            auth.getName(),
            solicitud.nombre(),
            solicitud.descripcion(),
            solicitud.montoInversion(),
            fechaFin,
            solicitud.radicacionId(),
            solicitud.responsableId()
        ));
    }

    @PatchMapping("/{id}/estado")
    public RespuestaProyecto actualizarEstado(
        @PathVariable Long id,
        @RequestBody Map<String, String> cuerpo,
        Authentication auth
    ) {
        return mapeador.aRespuesta(servicio.actualizarEstado(auth.getName(), id, cuerpo.get("estado")));
    }

    @GetMapping("/alertas")
    public List<RespuestaHitoObra> listarAlertas() {
        return servicio.listarHitosVencidos().stream()
            .map(mapeador::aRespuestaHito)
            .toList();
    }

    @PostMapping("/{id}/hitos")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaHitoObra agregarHito(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudHitoObra solicitud,
        Authentication auth
    ) {
        LocalDate fechaVenc = solicitud.fechaVencimiento() != null
            ? LocalDate.parse(solicitud.fechaVencimiento())
            : null;
        return mapeador.aRespuestaHito(servicio.agregarHito(auth.getName(), id, solicitud.descripcion(), fechaVenc));
    }

    @PatchMapping("/{id}/hitos/{hitoId}/cumplido")
    public RespuestaHitoObra marcarCumplido(
        @PathVariable Long id,
        @PathVariable Long hitoId,
        Authentication auth
    ) {
        return mapeador.aRespuestaHito(servicio.marcarHitoCumplido(auth.getName(), id, hitoId));
    }

    @DeleteMapping("/{id}/hitos/{hitoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarHito(
        @PathVariable Long id,
        @PathVariable Long hitoId,
        Authentication auth
    ) {
        servicio.eliminarHito(auth.getName(), id, hitoId);
    }
}
