package com.gpiv.atlanticsprinttech.backend.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCambioEstadoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudObservacionRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.TipoDocumentoRadicacion;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/api/radicaciones")
public class ControladorRadicacion {

    private final ServicioRadicacion servicioRadicacion;
    private final ObjectMapper objectMapper;

    public ControladorRadicacion(ServicioRadicacion servicioRadicacion, ObjectMapper objectMapper) {
        this.servicioRadicacion = servicioRadicacion;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<RespuestaRadicacion> listar(
        Authentication autenticacion,
        @RequestParam(value = "estado", required = false) EstadoRadicacion estado,
        @RequestParam(value = "desde", required = false) LocalDate desde,
        @RequestParam(value = "hasta", required = false) LocalDate hasta
    ) {
        return servicioRadicacion.listar(autenticacion.getName(), estado, desde, hasta).stream()
            .map(this::crearRespuesta)
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaRadicacion obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return crearRespuesta(servicioRadicacion.obtenerPorId(autenticacion.getName(), id));
    }

    @PostMapping
    public ResponseEntity<RespuestaRadicacion> crear(@Valid @RequestBody SolicitudRadicacion solicitud, Authentication autenticacion) {
        RadicacionSolicitud creada = servicioRadicacion.crear(
            autenticacion.getName(),
            solicitud.tipoSolicitud(),
            solicitud.descripcion(),
            solicitud.usoEstimativo(),
            solicitud.relevamientoPedidoLotes()
        );
        return ResponseEntity.created(URI.create("/api/radicaciones/" + creada.getId())).body(crearRespuesta(creada));
    }

    @PatchMapping("/{id}/estado")
    public RespuestaRadicacion cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudCambioEstadoRadicacion solicitud,
        Authentication autenticacion
    ) {
        RadicacionSolicitud actualizada = servicioRadicacion.cambiarEstado(
            autenticacion.getName(),
            id,
            solicitud.estado(),
            solicitud.comentario(),
            solicitud.tiempoEstimadoObraMeses(),
            solicitud.fechaPlazo()
        );
        return crearRespuesta(actualizada);
    }

    @PostMapping("/{id}/observaciones")
    public RespuestaOperacion registrarObservacion(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudObservacionRadicacion solicitud,
        Authentication autenticacion
    ) {
        servicioRadicacion.registrarObservacion(autenticacion.getName(), id, solicitud.comentario());
        return new RespuestaOperacion("Observacion registrada");
    }

    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaDocumentoRadicacion adjuntarDocumento(
        @PathVariable Long id,
        @RequestParam("tipoDocumento") TipoDocumentoRadicacion tipoDocumento,
        @RequestParam(value = "descripcion", required = false) String descripcion,
        @RequestParam("archivo") MultipartFile archivo,
        Authentication autenticacion
    ) throws Exception {
        RadicacionDocumento documento = servicioRadicacion.adjuntarDocumento(
            autenticacion.getName(),
            id,
            tipoDocumento,
            descripcion,
            archivo.getOriginalFilename(),
            archivo.getContentType(),
            archivo.getBytes()
        );
        return crearRespuestaDocumento(documento);
    }

    @GetMapping("/{id}/documentos")
    public List<RespuestaDocumentoRadicacion> listarDocumentos(@PathVariable Long id, Authentication autenticacion) {
        return servicioRadicacion.listarDocumentos(autenticacion.getName(), id).stream()
            .map(this::crearRespuestaDocumento)
            .toList();
    }

    @GetMapping("/{id}/documentos/{docId}/descargar")
    public ResponseEntity<byte[]> descargarDocumento(
        @PathVariable Long id,
        @PathVariable Long docId,
        Authentication autenticacion
    ) {
        RadicacionDocumento doc = servicioRadicacion.obtenerDocumento(autenticacion.getName(), id, docId);
        String contentDisposition = "attachment; filename=\"" + doc.getNombreArchivo().replace("\"", "") + "\"";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(doc.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .body(doc.getContenido());
    }

    @GetMapping("/{id}/historial")
    public List<RespuestaHistorialRadicacion> listarHistorial(@PathVariable Long id, Authentication autenticacion) {
        return servicioRadicacion.listarHistorial(autenticacion.getName(), id).stream()
            .map(this::crearRespuestaHistorial)
            .toList();
    }

    private RespuestaRadicacion crearRespuesta(RadicacionSolicitud radicacion) {
        return new RespuestaRadicacion(
            radicacion.getId(),
            radicacion.getNumeroRadicado(),
            radicacion.getEmpresa().getId(),
            radicacion.getEmpresa().getNombre(),
            radicacion.getTipoSolicitud(),
            radicacion.getDescripcion(),
            radicacion.getUsoEstimativo(),
            radicacion.getRelevamientoPedidoLotesJson() != null,
            obtenerEtapaActual(radicacion.getEstado()),
            radicacion.getEstado(),
            radicacion.getFechaRadicacion(),
            radicacion.getFechaUltimaActualizacion(),
            radicacion.getTiempoEstimadoObraMeses(),
            radicacion.getFechaPlazo(),
            radicacion.getFechaAprobacion(),
            extraerTiempoSolicitadoMeses(radicacion.getRelevamientoPedidoLotesJson())
        );
    }

    private Integer extraerTiempoSolicitadoMeses(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode nodo = objectMapper.readTree(json);
            JsonNode campo = nodo.get("tiempoRadicacionMeses");
            return (campo != null && !campo.isNull()) ? campo.asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer obtenerEtapaActual(EstadoRadicacion estado) {
        return switch (estado) {
            case PENDIENTE, EN_REVISION, REQUIERE_INFORMACION_ADICIONAL -> 2;
            case APROBADA, RADICADA, RECHAZADA, CANCELADA -> 3;
        };
    }

    private RespuestaDocumentoRadicacion crearRespuestaDocumento(RadicacionDocumento documento) {
        return new RespuestaDocumentoRadicacion(
            documento.getId(),
            documento.getTipoDocumento(),
            documento.getNombreArchivo(),
            documento.getMimeType(),
            documento.getTamanoBytes(),
            documento.getDescripcion(),
            documento.getSubidoPor(),
            documento.getFechaSubida()
        );
    }

    private RespuestaHistorialRadicacion crearRespuestaHistorial(RadicacionHistorial historial) {
        return new RespuestaHistorialRadicacion(
            historial.getId(),
            historial.getEstado(),
            historial.getComentario(),
            historial.getUsuario(),
            historial.getFechaEvento()
        );
    }
}
