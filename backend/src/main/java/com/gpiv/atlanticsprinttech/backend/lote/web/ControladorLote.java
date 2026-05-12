package com.gpiv.atlanticsprinttech.backend.lote.web;

import com.gpiv.atlanticsprinttech.backend.lote.application.ServicioLote;
import com.gpiv.atlanticsprinttech.commons.lote.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.lote.dto.SolicitudLote;
import com.gpiv.atlanticsprinttech.commons.compartido.dto.RespuestaPaginada;
import com.gpiv.atlanticsprinttech.entities.lote.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/lotes")
public class ControladorLote {

    private static final String EMPRESA_SIN_ASIGNAR = "Sin asignar";

    private final ServicioLote servicioLote;

    public ControladorLote(ServicioLote servicioLote) {
        this.servicioLote = servicioLote;
    }

    @GetMapping
    public RespuestaPaginada<RespuestaLote> listar(
        Authentication autenticacion,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "50") int tamanio
    ) {
        var paginable = PageRequest.of(pagina, tamanio, Sort.by("codigo").ascending());
        return RespuestaPaginada.de(servicioLote.listar(autenticacion.getName(), paginable).map(this::crearRespuesta));
    }

    @GetMapping("/{id}")
    public RespuestaLote obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return crearRespuesta(servicioLote.obtenerPorId(id, autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<RespuestaLote> crear(@Valid @RequestBody SolicitudLote solicitud, Authentication autenticacion) {
        Lote loteCreado = servicioLote.crear(
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.empresaId(),
            solicitud.estadoAsignacion(),
            solicitud.numeroExpedienteReferencia(),
            solicitud.zona(),
            autenticacion.getName()
        );
        return ResponseEntity.created(URI.create("/api/lotes/" + loteCreado.getId()))
            .body(crearRespuesta(loteCreado));
    }

    @PutMapping("/{id}")
    public RespuestaLote actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudLote solicitud, Authentication autenticacion) {
        Lote loteActualizado = servicioLote.actualizar(
            id,
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.empresaId(),
            solicitud.estadoAsignacion(),
            solicitud.numeroExpedienteReferencia(),
            solicitud.zona(),
            autenticacion.getName()
        );
        return crearRespuesta(loteActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication autenticacion) {
        servicioLote.eliminar(id, autenticacion.getName());
        return ResponseEntity.noContent().build();
    }

    private RespuestaLote crearRespuesta(Lote lote) {
        Long empresaId = lote.getEmpresa() != null ? lote.getEmpresa().getId() : null;
        String nombreEmpresa = lote.getEmpresa() != null ? lote.getEmpresa().getNombre() : EMPRESA_SIN_ASIGNAR;
        String cuitEmpresa = lote.getEmpresa() != null ? lote.getEmpresa().getCuit() : null;
        String estadoAsignacion = (lote.getEstadoAsignacion() != null && lote.getEstadoAsignacion() != EstadoAsignacionLote.DISPONIBLE)
            ? lote.getEstadoAsignacion().name() : null;
        String fechaAsignacion = lote.getFechaAsignacion() != null
            ? lote.getFechaAsignacion().format(DateTimeFormatter.ISO_LOCAL_DATE)
            : null;
        String zona = lote.getZona() != null ? lote.getZona().name() : null;
        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.estaOcupado(),
            empresaId,
            nombreEmpresa,
            cuitEmpresa,
            estadoAsignacion,
            fechaAsignacion,
            lote.getNumeroExpedienteReferencia(),
            lote.getPoligono(),
            zona
        );
    }
}
