package com.gpiv.atlanticsprinttech.backend.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorRadicacion;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudAsignacionLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCambioEstadoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudObservacionRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
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
    private final MapeadorRadicacion mapeador;
    private final ObjectMapper objectMapper;

    public ControladorRadicacion(
        ServicioRadicacion servicioRadicacion,
        MapeadorRadicacion mapeador,
        ObjectMapper objectMapper
    ) {
        this.servicioRadicacion = servicioRadicacion;
        this.mapeador = mapeador;
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
            .map(mapeador::toRespuesta)
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaRadicacion obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return mapeador.toRespuesta(servicioRadicacion.obtenerPorId(autenticacion.getName(), id));
    }

    @PostMapping
    public ResponseEntity<RespuestaRadicacion> crear(
        @Valid @RequestBody SolicitudRadicacion solicitud,
        Authentication autenticacion
    ) {
        RadicacionSolicitud creada = servicioRadicacion.crear(
            autenticacion.getName(),
            solicitud.tipoSolicitud(),
            solicitud.descripcion(),
            solicitud.usoEstimativo(),
            solicitud.relevamientoPedidoLotes()
        );
        return ResponseEntity
            .created(URI.create("/api/radicaciones/" + creada.getId()))
            .body(mapeador.toRespuesta(creada));
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
            solicitud.fechaPlazo(),
            solicitud.fechaAprobacion(),
            solicitud.numeroResolucion()
        );
        return mapeador.toRespuesta(actualizada);
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
            autenticacion.getName(), id, tipoDocumento, descripcion,
            archivo.getOriginalFilename(), archivo.getContentType(), archivo.getBytes()
        );
        return mapeador.toDocumentoRespuesta(documento);
    }

    @GetMapping("/{id}/documentos")
    public List<RespuestaDocumentoRadicacion> listarDocumentos(
        @PathVariable Long id,
        Authentication autenticacion
    ) {
        return servicioRadicacion.listarDocumentos(autenticacion.getName(), id).stream()
            .map(mapeador::toDocumentoRespuesta)
            .toList();
    }

    @GetMapping("/{id}/documentos/{docId}")
    public ResponseEntity<byte[]> verDocumento(
        @PathVariable Long id,
        @PathVariable Long docId,
        Authentication autenticacion
    ) {
        RadicacionDocumento doc = servicioRadicacion.obtenerDocumento(autenticacion.getName(), id, docId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(doc.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombreArchivo().replace("\"", "") + "\"")
            .body(doc.getContenido());
    }

    @GetMapping("/{id}/documentos/{docId}/descargar")
    public ResponseEntity<byte[]> descargarDocumento(
        @PathVariable Long id,
        @PathVariable Long docId,
        Authentication autenticacion
    ) {
        RadicacionDocumento doc = servicioRadicacion.obtenerDocumento(autenticacion.getName(), id, docId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(doc.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNombreArchivo().replace("\"", "") + "\"")
            .body(doc.getContenido());
    }

    @GetMapping("/{id}/historial")
    public List<RespuestaHistorialRadicacion> listarHistorial(
        @PathVariable Long id,
        Authentication autenticacion
    ) {
        return servicioRadicacion.listarHistorial(autenticacion.getName(), id).stream()
            .map(mapeador::toHistorialRespuesta)
            .toList();
    }

    @GetMapping("/{id}/relevamiento")
    public ResponseEntity<JsonNode> obtenerRelevamiento(@PathVariable Long id, Authentication autenticacion) {
        String json = servicioRadicacion.obtenerPorId(autenticacion.getName(), id)
            .getRelevamientoPedidoLotesJson();
        if (json == null || json.isBlank()) return ResponseEntity.noContent().build();
        try {
            return ResponseEntity.ok(objectMapper.readTree(json));
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/{id}/lote")
    public ResponseEntity<RespuestaLote> obtenerLote(@PathVariable Long id, Authentication autenticacion) {
        Lote lote = servicioRadicacion.obtenerPorId(autenticacion.getName(), id).getLote();
        return lote == null
            ? ResponseEntity.noContent().build()
            : ResponseEntity.ok(mapeador.toLoteRespuesta(lote));
    }

    @PostMapping(value = "/{id}/rubrica", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaDocumentoRadicacion subirActaRubrica(
        @PathVariable Long id,
        @RequestParam("archivo") MultipartFile archivo,
        Authentication autenticacion
    ) throws Exception {
        RadicacionDocumento acta = servicioRadicacion.subirActaRubrica(
            autenticacion.getName(), id,
            archivo.getOriginalFilename(), archivo.getContentType(), archivo.getBytes()
        );
        return mapeador.toDocumentoRespuesta(acta);
    }

    @GetMapping("/{id}/rubrica")
    public ResponseEntity<byte[]> obtenerActaRubrica(@PathVariable Long id, Authentication autenticacion) {
        RadicacionDocumento acta = servicioRadicacion.obtenerActaRubrica(autenticacion.getName(), id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(acta.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + acta.getNombreArchivo().replace("\"", "") + "\"")
            .body(acta.getContenido());
    }

    @PatchMapping("/{id}/lote")
    public RespuestaRadicacion asignarLote(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudAsignacionLote solicitud,
        Authentication autenticacion
    ) {
        return mapeador.toRespuesta(
            servicioRadicacion.asignarLote(autenticacion.getName(), id, solicitud.loteId())
        );
    }
}
