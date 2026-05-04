package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
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
@RequestMapping("/api/empresas")
public class ControladorEmpresa {

    private final ServicioEmpresa servicioEmpresa;
    public ControladorEmpresa(ServicioEmpresa servicioEmpresa) {
        this.servicioEmpresa = servicioEmpresa;
    }
    @GetMapping
    public List<RespuestaEmpresa> listar() {
        return servicioEmpresa.listar().stream().map(this::crearRespuesta).toList();
    }
    @GetMapping("/{id}")
    public RespuestaEmpresa obtenerPorId(@PathVariable Long id) {
        return crearRespuesta(servicioEmpresa.obtenerPorId(id));
    }
    @PostMapping
    public ResponseEntity<RespuestaEmpresa> crear(@Valid @RequestBody SolicitudEmpresa solicitud) {
        Empresa empresaCreada = servicioEmpresa.crear(crearEmpresa(solicitud));
        return ResponseEntity.created(URI.create("/api/empresas/" + empresaCreada.getId()))
            .body(crearRespuesta(empresaCreada));
    }
    @PutMapping("/{id}")
    public RespuestaEmpresa actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudEmpresa solicitud) {
        Empresa empresaActualizada = servicioEmpresa.actualizar(id, crearEmpresa(solicitud));
        return crearRespuesta(empresaActualizada);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioEmpresa.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    private Empresa crearEmpresa(SolicitudEmpresa solicitud) {
        return Empresa.crear(solicitud.nombre(), solicitud.cuit(), solicitud.correoElectronico());
    }
    private RespuestaEmpresa crearRespuesta(Empresa empresa) {
        return new RespuestaEmpresa(
            empresa.getId(),
            empresa.getNombre(),
            empresa.getCuit(),
            empresa.getCorreoElectronico()
        );
    }
    @PostMapping("/{id}/solicitudes-cambio-rubro")
    public ResponseEntity<RespuestaOperacion> solicitarCambioRubro(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudCambioRubro solicitud
            ) {
        servicioEmpresa.solicitarAmpliacionOCambioRubro(id, solicitud.idNuevoRubro(), solicitud.justificacion());
        return ResponseEntity.accepted().body(new RespuestaOperacion(
                "Solicitud de cambio de rubro generada y pendiente de aprobación."
        ));
    }
}