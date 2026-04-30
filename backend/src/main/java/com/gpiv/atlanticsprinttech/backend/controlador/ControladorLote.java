package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotes")
public class ControladorLote {

    private final ServicioLote servicioLote;

    public ControladorLote(ServicioLote servicioLote) {
        this.servicioLote = servicioLote;
    }

    @GetMapping
    public List<RespuestaLote> listar() {
        return servicioLote.listar().stream().map(this::crearRespuesta).toList();
    }

    @GetMapping("/{id}")
    public RespuestaLote obtenerPorId(@PathVariable Long id) {
        return crearRespuesta(servicioLote.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<RespuestaLote> crear(@Valid @RequestBody SolicitudLote solicitud) {
        Lote loteCreado = servicioLote.crear(
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.ocupado(),
            solicitud.empresaId()
        );
        return ResponseEntity.created(URI.create("/api/lotes/" + loteCreado.getId()))
            .body(crearRespuesta(loteCreado));
    }

    @PutMapping("/{id}")
    public RespuestaLote actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudLote solicitud) {
        Lote loteActualizado = servicioLote.actualizar(
            id,
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.ocupado(),
            solicitud.empresaId()
        );
        return crearRespuesta(loteActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioLote.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private RespuestaLote crearRespuesta(Lote lote) {
        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.isOcupado(),
            lote.getEmpresa().getId(),
            lote.getEmpresa().getNombre()
        );
    }
}

