package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudLote;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final MapeadorLote mapeador;

    public ControladorLote(ServicioLote servicioLote, MapeadorLote mapeador) {
        this.servicioLote = servicioLote;
        this.mapeador = mapeador;
    }

    @GetMapping
    public List<RespuestaLote> listar(Authentication autenticacion) {
        return servicioLote.listar(autenticacion.getName()).stream()
            .map(mapeador::aRespuesta)
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaLote obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return mapeador.aRespuesta(servicioLote.obtenerPorId(id, autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<RespuestaLote> crear(@Valid @RequestBody SolicitudLote solicitud, Authentication autenticacion) {
        var loteCreado = servicioLote.crear(
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.ocupado(),
            solicitud.empresaId(),
            solicitud.estadoAsignacion(),
            solicitud.numeroExpedienteReferencia(),
            solicitud.zona(),
            autenticacion.getName()
        );
        return ResponseEntity.created(URI.create("/api/lotes/" + loteCreado.getId()))
            .body(mapeador.aRespuesta(loteCreado));
    }

    @PutMapping("/{id}")
    public RespuestaLote actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudLote solicitud, Authentication autenticacion) {
        return mapeador.aRespuesta(servicioLote.actualizar(
            id,
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.ocupado(),
            solicitud.empresaId(),
            solicitud.estadoAsignacion(),
            solicitud.numeroExpedienteReferencia(),
            solicitud.zona(),
            autenticacion.getName()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication autenticacion) {
        servicioLote.eliminar(id, autenticacion.getName());
        return ResponseEntity.noContent().build();
    }
}
