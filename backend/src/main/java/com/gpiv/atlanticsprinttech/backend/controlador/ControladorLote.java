package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioCicloLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAlertaVencimiento;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialEtapa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaReservaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudTransicionEtapa;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final ServicioCicloLote servicioCicloLote;
    private final MapeadorLote mapeador;

    public ControladorLote(ServicioLote servicioLote, ServicioCicloLote servicioCicloLote, MapeadorLote mapeador) {
        this.servicioLote = servicioLote;
        this.servicioCicloLote = servicioCicloLote;
        this.mapeador = mapeador;
    }

    @GetMapping
    public List<RespuestaLote> listar(Authentication autenticacion) {
        return servicioLote.listar(autenticacion.getName()).stream()
            .map(mapeador::aRespuesta)
            .toList();
    }

    @GetMapping("/disponibles")
    public List<RespuestaLote> listarDisponibles() {
        return servicioLote.listarDisponibles().stream()
            .map(mapeador::aRespuesta)
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaLote obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return mapeador.aRespuesta(servicioLote.obtenerPorId(id, autenticacion.getName()));
    }

    @GetMapping("/{id}/radicaciones")
    public List<RespuestaReservaLote> listarReservas(@PathVariable Long id) {
        return servicioLote.listarReservas(id);
    }

    @PostMapping
    public ResponseEntity<RespuestaLote> crear(@Valid @RequestBody SolicitudLote solicitud, Authentication autenticacion) {
        var loteCreado = servicioLote.crear(
            solicitud.codigo(),
            solicitud.superficieMetrosCuadrados(),
            solicitud.ocupado(),
            solicitud.empresaId(),
            null,
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
            null,
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

    @PatchMapping("/{id}/color")
    public RespuestaLote actualizarColor(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication autenticacion) {
        return mapeador.aRespuesta(servicioLote.actualizarColor(id, body.get("color"), autenticacion.getName()));
    }

    @PostMapping("/{id}/transicionar")
    public RespuestaLote transicionar(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudTransicionEtapa solicitud,
        Authentication autenticacion
    ) {
        return servicioCicloLote.transicionar(id, solicitud.etapa(), solicitud.motivo(), autenticacion.getName());
    }

    @GetMapping("/{id}/historial")
    public List<RespuestaHistorialEtapa> listarHistorial(@PathVariable Long id, Authentication autenticacion) {
        return servicioCicloLote.listarHistorial(id, autenticacion.getName());
    }

    @GetMapping("/alertas-vencimiento")
    public List<RespuestaAlertaVencimiento> listarAlertasVencimiento() {
        return servicioCicloLote.listarAlertasVencimiento();
    }
}
