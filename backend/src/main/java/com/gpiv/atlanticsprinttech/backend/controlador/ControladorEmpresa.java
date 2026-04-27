package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
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
@RequestMapping("/api/empresas")
public class ControladorEmpresa {

    private final ServicioEmpresa servicioEmpresa;
    public ControladorEmpresa(ServicioEmpresa servicioEmpresa) {
        this.servicioEmpresa = servicioEmpresa;
    }
    @GetMapping
    public List<RespuestaEmpresa> listar(Authentication autenticacion) {
        return servicioEmpresa.listar(autenticacion.getName()).stream().map(this::crearRespuesta).toList();
    }
    @GetMapping("/{id}")
    public RespuestaEmpresa obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return crearRespuesta(servicioEmpresa.obtenerPorId(id, autenticacion.getName()));
    }
    @PostMapping
    public ResponseEntity<RespuestaEmpresa> crear(@Valid @RequestBody SolicitudEmpresa solicitud, Authentication autenticacion) {
        Empresa empresaCreada = servicioEmpresa.crear(crearEmpresa(solicitud), autenticacion.getName());
        return ResponseEntity.created(URI.create("/api/empresas/" + empresaCreada.getId()))
            .body(crearRespuesta(empresaCreada));
    }
    @PutMapping("/{id}")
    public RespuestaEmpresa actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudEmpresa solicitud, Authentication autenticacion) {
        Empresa empresaActualizada = servicioEmpresa.actualizar(id, crearEmpresa(solicitud), autenticacion.getName());
        return crearRespuesta(empresaActualizada);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication autenticacion) {
        servicioEmpresa.eliminar(id, autenticacion.getName());
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
}