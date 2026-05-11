package com.gpiv.atlanticsprinttech.backend.radicacion.web;

import com.gpiv.atlanticsprinttech.backend.radicacion.application.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.backend.util.JsonUtil;
import com.gpiv.atlanticsprinttech.commons.lote.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.RespuestaDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.RespuestaHistorialRadicacion;
import com.gpiv.atlanticsprinttech.commons.shared.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.RespuestaRadicacion;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.SolicitudAsignacionLote;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.SolicitudCambioEstadoRadicacion;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.SolicitudObservacionRadicacion;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.SolicitudRadicacion;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.radicacion.TipoDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.shared.dto.RespuestaPaginada;
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
@RequestMapping("/api/radicaciones")
public class ControladorRadicacion {

    private final ServicioRadicacion servicioRadicacion;

    public ControladorRadicacion(ServicioRadicacion servicioRadicacion) {
        this.servicioRadicacion = servicioRadicacion;
    }

    @GetMapping
    public RespuestaPaginada<RespuestaRadicacion> listar(
        Authentication autenticacion,
        @RequestParam(value = "estado", required = false) EstadoRadicacion estado,
        @RequestParam(value = "desde", required = false) LocalDate desde,
        @RequestParam(value = "hasta", required = false) LocalDate hasta,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "50") int tamanio
    ) {
        var paginable = PageRequest.of(pagina, tamanio, Sort.by("fechaUltimaActualizacion").descending());
        return RespuestaPaginada.de(
            servicioRadicacion.listar(autenticacion.getName(), estado, desde, hasta, paginable)
                .map(this::crearRespuesta)
        );
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
            solicitud.comentario()
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

    @GetMapping("/{id}/historial")
    public List<RespuestaHistorialRadicacion> listarHistorial(@PathVariable Long id, Authentication autenticacion) {
        return servicioRadicacion.listarHistorial(autenticacion.getName(), id).stream()
            .map(this::crearRespuestaHistorial)
            .toList();
    }

    @PostMapping("/{id}/lote")
    public ResponseEntity<RespuestaOperacion> asignarLote(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudAsignacionLote solicitud,
        Authentication autenticacion
    ) {
        servicioRadicacion.asignarLote(autenticacion.getName(), id, solicitud.loteId());
        return ResponseEntity.ok(new RespuestaOperacion("Lote reservado para la solicitud"));
    }

    @GetMapping("/{id}/lote")
    public ResponseEntity<RespuestaLote> obtenerLoteAsignado(@PathVariable Long id, Authentication autenticacion) {
        return servicioRadicacion.obtenerLoteAsignado(autenticacion.getName(), id)
            .map(this::crearRespuestaLote)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
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
            JsonUtil.extraerNecesidadMetrosCuadrados(radicacion.getRelevamientoPedidoLotesJson()),
            obtenerEtapaActual(radicacion.getEstado()),
            radicacion.getEstado(),
            radicacion.getFechaRadicacion(),
            radicacion.getFechaUltimaActualizacion()
        );
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
            historial.getEstadoAnterior(),
            historial.getEstado(),
            historial.getComentario(),
            historial.getUsuario(),
            historial.getFechaEvento()
        );
    }

    private RespuestaLote crearRespuestaLote(Lote lote) {
        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.estaOcupado(),
            lote.getEmpresaId(),
            lote.getEmpresa() != null ? lote.getEmpresa().getNombre() : null,
            lote.getEmpresa() != null ? lote.getEmpresa().getCuit() : null,
            lote.getEstadoAsignacion() != null ? lote.getEstadoAsignacion().name() : null,
            lote.getFechaAsignacion() != null ? lote.getFechaAsignacion().toString() : null,
            lote.getNumeroExpedienteReferencia(),
            lote.getPoligono(),
            lote.getZona() != null ? lote.getZona().name() : null
        );
    }
}
