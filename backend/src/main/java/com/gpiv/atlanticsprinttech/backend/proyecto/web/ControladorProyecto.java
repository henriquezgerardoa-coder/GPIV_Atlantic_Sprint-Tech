package com.gpiv.atlanticsprinttech.backend.proyecto.web;

import com.gpiv.atlanticsprinttech.backend.proyecto.application.ServicioProyecto;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.RespuestaDocumentoPDF;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.RespuestaHitoObra;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.RespuestaProyecto;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.SolicitudCambioEstadoProyecto;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.SolicitudCrearProyecto;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.SolicitudHitoObra;
import com.gpiv.atlanticsprinttech.commons.proyecto.dto.SolicitudIniciarProyecto;
import com.gpiv.atlanticsprinttech.entities.proyecto.DocumentoPDF;
import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.proyecto.HitoObra;
import com.gpiv.atlanticsprinttech.entities.proyecto.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.commons.compartido.dto.RespuestaPaginada;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/proyectos")
public class ControladorProyecto {

    private final ServicioProyecto servicioProyecto;

    public ControladorProyecto(ServicioProyecto servicioProyecto) {
        this.servicioProyecto = servicioProyecto;
    }

    @GetMapping
    public RespuestaPaginada<RespuestaProyecto> listar(
        Authentication autenticacion,
        @RequestParam(required = false) EstadoProyecto estado,
        @RequestParam(required = false) LocalDate desde,
        @RequestParam(required = false) LocalDate hasta,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "50") int tamanio
    ) {
        var paginable = PageRequest.of(pagina, tamanio, Sort.by("fechaCreacion").descending());
        return RespuestaPaginada.de(
            servicioProyecto.listar(autenticacion.getName(), estado, desde, hasta, paginable)
                .map(this::crearRespuesta)
        );
    }

    @GetMapping("/{id}")
    public RespuestaProyecto obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return crearRespuesta(servicioProyecto.obtenerPorId(autenticacion.getName(), id));
    }

    @PostMapping
    public ResponseEntity<RespuestaProyecto> crear(
        @Valid @RequestBody SolicitudCrearProyecto solicitud,
        Authentication autenticacion
    ) {
        ProyectoProductivo creado = servicioProyecto.crear(
            autenticacion.getName(),
            solicitud.nombre(),
            solicitud.descripcion(),
            solicitud.fechaEstimadaFin(),
            solicitud.montoInversion(),
            solicitud.loteId(),
            solicitud.responsableSeguimientoId()
        );
        return ResponseEntity.created(URI.create("/api/proyectos/" + creado.getId())).body(crearRespuesta(creado));
    }

    @PostMapping("/{id}/iniciar")
    public RespuestaProyecto iniciarProyecto(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudIniciarProyecto solicitud,
        Authentication autenticacion
    ) {
        return crearRespuesta(servicioProyecto.iniciarProyecto(autenticacion.getName(), id, solicitud.radicacionId()));
    }

    @PatchMapping("/{id}/estado")
    public RespuestaProyecto actualizarEstado(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudCambioEstadoProyecto solicitud,
        Authentication autenticacion
    ) {
        return crearRespuesta(servicioProyecto.actualizarEstado(autenticacion.getName(), id, solicitud.estado()));
    }

    @GetMapping("/{id}/hitos")
    public List<RespuestaHitoObra> listarHitos(@PathVariable Long id, Authentication autenticacion) {
        return servicioProyecto.listarHitos(autenticacion.getName(), id).stream()
            .map(this::crearRespuestaHito)
            .toList();
    }

    @PostMapping("/{id}/hitos")
    public ResponseEntity<RespuestaHitoObra> agregarHito(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudHitoObra solicitud,
        Authentication autenticacion
    ) {
        HitoObra hito = servicioProyecto.agregarHito(
            autenticacion.getName(), id, solicitud.descripcion(), solicitud.fechaVencimientoReal()
        );
        return ResponseEntity.created(URI.create("/api/proyectos/" + id + "/hitos/" + hito.getId()))
            .body(crearRespuestaHito(hito));
    }

    @PatchMapping("/{id}/hitos/{hitoId}/cumplido")
    public RespuestaHitoObra marcarHitoCumplido(
        @PathVariable Long id,
        @PathVariable Long hitoId,
        Authentication autenticacion
    ) {
        return crearRespuestaHito(servicioProyecto.marcarHitoCumplido(autenticacion.getName(), id, hitoId));
    }

    @GetMapping("/{id}/documentos")
    public List<RespuestaDocumentoPDF> listarDocumentos(@PathVariable Long id, Authentication autenticacion) {
        return servicioProyecto.listarDocumentos(autenticacion.getName(), id).stream()
            .map(this::crearRespuestaDocumento)
            .toList();
    }

    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaDocumentoPDF> adjuntarDocumento(
        @PathVariable Long id,
        @RequestParam("archivo") MultipartFile archivo,
        Authentication autenticacion
    ) throws Exception {
        DocumentoPDF doc = servicioProyecto.adjuntarDocumento(
            autenticacion.getName(), id, archivo.getOriginalFilename(), archivo.getBytes()
        );
        return ResponseEntity.created(URI.create("/api/proyectos/" + id + "/documentos/" + doc.getId()))
            .body(crearRespuestaDocumento(doc));
    }

    private RespuestaProyecto crearRespuesta(ProyectoProductivo p) {
        return new RespuestaProyecto(
            p.getId(),
            p.getNombre(),
            p.getEstado(),
            p.getDescripcion(),
            p.getFechaInicioReal(),
            p.getFechaEstimadaFin(),
            p.getMontoInversion(),
            p.getFechaCreacion(),
            p.getEmpresa().getId(),
            p.getEmpresa().getNombre(),
            p.getLote().getId(),
            p.getLote().getCodigo(),
            p.getResponsableSeguimiento().getNombreCompleto(),
            p.getRadicacionOrigen() != null ? p.getRadicacionOrigen().getId() : null,
            p.calcularAvanceFisico(),
            p.validarVencimientoPlazo()
        );
    }

    private RespuestaHitoObra crearRespuestaHito(HitoObra h) {
        return new RespuestaHitoObra(
            h.getId(),
            h.getDescripcion(),
            h.getFechaVencimientoReal(),
            h.estaCumplido(),
            h.estaVencido()
        );
    }

    private RespuestaDocumentoPDF crearRespuestaDocumento(DocumentoPDF d) {
        return new RespuestaDocumentoPDF(
            d.getId(),
            d.getNombreArchivo(),
            d.getFechaCarga(),
            d.getTamanoBytes()
        );
    }
}