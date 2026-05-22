package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaCensoVehiculo;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaPersonalCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCensoVehiculo;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudPersonalCenso;
import com.gpiv.atlanticsprinttech.entities.dominio.Censo;
import com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado;
import com.gpiv.atlanticsprinttech.entities.dominio.Vehiculo;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas/{empresaId}/censo")
public class ControladorCenso {

    private final ServicioCenso servicioCenso;

    public ControladorCenso(ServicioCenso servicioCenso) {
        this.servicioCenso = servicioCenso;
    }

    // ── Declaraciones anuales ──────────────────────────────────────────────

    @GetMapping
    public List<RespuestaCenso> listarCensos(@PathVariable Long empresaId, Authentication auth) {
        return servicioCenso.listarCensos(auth.getName(), empresaId).stream()
            .map(this::toRespuestaCenso)
            .toList();
    }

    @PostMapping
    public RespuestaCenso declararCenso(
        @PathVariable Long empresaId,
        @Valid @RequestBody SolicitudCenso solicitud,
        Authentication auth
    ) {
        return toRespuestaCenso(servicioCenso.declararCenso(
            auth.getName(), empresaId,
            solicitud.anioPeriodo(),
            solicitud.cantidadPersonalRegistrado(),
            solicitud.cantidadPersonasNoRegistrado(),
            solicitud.observacion()
        ));
    }

    // ── Personal registrado ────────────────────────────────────────────────

    @GetMapping("/personal")
    public List<RespuestaPersonalCenso> listarPersonal(@PathVariable Long empresaId, Authentication auth) {
        return servicioCenso.listarPersonal(auth.getName(), empresaId).stream()
            .map(this::toRespuestaPersonal)
            .toList();
    }

    @PostMapping("/personal")
    public RespuestaPersonalCenso agregarPersonal(
        @PathVariable Long empresaId,
        @Valid @RequestBody SolicitudPersonalCenso solicitud,
        Authentication auth
    ) {
        LocalDate fechaIngreso = LocalDate.parse(solicitud.fechaIngreso());
        return toRespuestaPersonal(servicioCenso.agregarPersonal(
            auth.getName(), empresaId,
            solicitud.cuit(), solicitud.nombreCompleto(), fechaIngreso
        ));
    }

    @DeleteMapping("/personal/{personalId}")
    public ResponseEntity<RespuestaOperacion> eliminarPersonal(
        @PathVariable Long empresaId,
        @PathVariable Long personalId,
        Authentication auth
    ) {
        servicioCenso.eliminarPersonal(auth.getName(), empresaId, personalId);
        return ResponseEntity.ok(new RespuestaOperacion("Empleado eliminado"));
    }

    // ── Vehículos ──────────────────────────────────────────────────────────

    @GetMapping("/vehiculos")
    public List<RespuestaCensoVehiculo> listarVehiculos(@PathVariable Long empresaId, Authentication auth) {
        return servicioCenso.listarVehiculos(auth.getName(), empresaId).stream()
            .map(this::toRespuestaVehiculo)
            .toList();
    }

    @PostMapping("/vehiculos")
    public RespuestaCensoVehiculo agregarVehiculo(
        @PathVariable Long empresaId,
        @Valid @RequestBody SolicitudCensoVehiculo solicitud,
        Authentication auth
    ) {
        return toRespuestaVehiculo(servicioCenso.agregarVehiculo(
            auth.getName(), empresaId,
            solicitud.patente(), solicitud.marcaModelo()
        ));
    }

    @DeleteMapping("/vehiculos/{vehiculoId}")
    public ResponseEntity<RespuestaOperacion> eliminarVehiculo(
        @PathVariable Long empresaId,
        @PathVariable Long vehiculoId,
        Authentication auth
    ) {
        servicioCenso.eliminarVehiculo(auth.getName(), empresaId, vehiculoId);
        return ResponseEntity.ok(new RespuestaOperacion("Vehículo eliminado"));
    }

    // ── Mapeo a DTOs ───────────────────────────────────────────────────────

    private RespuestaCenso toRespuestaCenso(Censo c) {
        return new RespuestaCenso(
            c.getId(),
            c.getAnioPeriodo(),
            c.getFechaDeclaracion().toString(),
            c.getCantidadPersonalRegistrado(),
            c.getCantidadPersonasNoRegistrado(),
            c.calcularTotalEmpleados(),
            c.getObservacion()
        );
    }

    private RespuestaPersonalCenso toRespuestaPersonal(PersonalRegistrado p) {
        return new RespuestaPersonalCenso(
            p.getId(),
            p.getCuit(),
            p.getNombreCompleto(),
            p.getFechaIngreso().toString()
        );
    }

    private RespuestaCensoVehiculo toRespuestaVehiculo(Vehiculo v) {
        return new RespuestaCensoVehiculo(
            v.getId(),
            v.getPatente(),
            v.getMarcaModelo(),
            v.validarFormatoPatente()
        );
    }
}
