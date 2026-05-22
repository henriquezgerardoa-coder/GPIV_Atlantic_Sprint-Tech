package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioProyecto;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHitoObra;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaProyecto;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudHitoObra;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
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

    public ControladorProyecto(ServicioProyecto servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaProyecto> listar(Authentication auth) {
        return servicio.listar(auth.getName()).stream()
            .map(this::toRespuesta)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaProyecto crear(@Valid @RequestBody SolicitudProyecto solicitud, Authentication auth) {
        LocalDate fechaFin = solicitud.fechaEstimadaFin() != null
            ? LocalDate.parse(solicitud.fechaEstimadaFin())
            : null;
        return toRespuesta(servicio.crear(
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
        @RequestBody Map<String, String> body,
        Authentication auth
    ) {
        String estado = body.get("estado");
        return toRespuesta(servicio.actualizarEstado(auth.getName(), id, estado));
    }

    @GetMapping("/alertas")
    public List<RespuestaHitoObra> listarAlertas() {
        return servicio.listarHitosVencidos().stream()
            .map(h -> toRespuestaHito(h, true))
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
        HitoObra hito = servicio.agregarHito(auth.getName(), id, solicitud.descripcion(), fechaVenc);
        return toRespuestaHito(hito, hito.estaVencido());
    }

    @PatchMapping("/{id}/hitos/{hitoId}/cumplido")
    public RespuestaHitoObra marcarCumplido(
        @PathVariable Long id,
        @PathVariable Long hitoId,
        Authentication auth
    ) {
        HitoObra hito = servicio.marcarHitoCumplido(auth.getName(), id, hitoId);
        return toRespuestaHito(hito, false);
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

    private RespuestaProyecto toRespuesta(ProyectoProductivo p) {
        var solicitud = p.getSolicitudOrigen();
        return new RespuestaProyecto(
            p.getId(),
            p.getNombre(),
            p.getDescripcion(),
            p.getEstado().name(),
            p.getFechaInicioReal() != null ? p.getFechaInicioReal().toString() : null,
            p.getFechaEstimadaFin() != null ? p.getFechaEstimadaFin().toString() : null,
            p.getMontoInversion(),
            p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null,
            solicitud != null ? solicitud.getId() : null,
            solicitud != null ? solicitud.getEmpresa().getNombre() : null,
            p.getResponsableSeguimiento() != null ? p.getResponsableSeguimiento().getNombreCompleto() : null,
            p.calcularAvanceFisico(),
            p.validarVencimientoPlazo(),
            p.getHitos().stream().map(h -> toRespuestaHito(h, h.estaVencido())).toList()
        );
    }

    private RespuestaHitoObra toRespuestaHito(HitoObra h, boolean vencido) {
        return new RespuestaHitoObra(
            h.getId(),
            h.getDescripcion(),
            h.getFechaVencimientoReal() != null ? h.getFechaVencimientoReal().toString() : null,
            h.isCumplido(),
            vencido
        );
    }
}
