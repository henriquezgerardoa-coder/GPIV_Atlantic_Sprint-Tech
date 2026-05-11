package com.gpiv.atlanticsprinttech.backend.empresa.web;

import com.gpiv.atlanticsprinttech.backend.empresa.application.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaEmpresa;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaEmpresaDetalleAdmin;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaEmpresaListadoAdmin;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.SolicitudEmpresa;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.SolicitudServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas")
public class ControladorEmpresa {

    private final ServicioEmpresa servicioEmpresa;
    public ControladorEmpresa(ServicioEmpresa servicioEmpresa) {
        this.servicioEmpresa = servicioEmpresa;
    }
    @GetMapping("/buscar")
    public List<RespuestaEmpresa> buscar(@RequestParam String q, Authentication autenticacion) {
        return servicioEmpresa.buscar(q).stream()
            .map(empresa -> crearRespuesta(empresa, autenticacion.getName()))
            .toList();
    }

    @GetMapping
    public List<RespuestaEmpresa> listar(Authentication autenticacion) {
        return servicioEmpresa.listar(autenticacion.getName()).stream()
            .map(empresa -> crearRespuesta(empresa, autenticacion.getName()))
            .toList();
    }
    @GetMapping("/{id}")
    public RespuestaEmpresa obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return crearRespuesta(servicioEmpresa.obtenerPorId(id, autenticacion.getName()), autenticacion.getName());
    }

    @GetMapping("/admin/vista")
    public List<RespuestaEmpresaListadoAdmin> listarVistaAdmin(Authentication autenticacion) {
        return servicioEmpresa.listarVistaAdmin(autenticacion.getName());
    }

    @GetMapping("/admin/vista/{id}")
    public RespuestaEmpresaDetalleAdmin obtenerDetalleVistaAdmin(@PathVariable Long id, Authentication autenticacion) {
        return servicioEmpresa.obtenerDetalleVistaAdmin(id, autenticacion.getName());
    }
    @PostMapping
    public ResponseEntity<RespuestaEmpresa> crear(@Valid @RequestBody SolicitudEmpresa solicitud, Authentication autenticacion) {
        Empresa empresaCreada = servicioEmpresa.crear(crearEmpresa(solicitud), autenticacion.getName());
        return ResponseEntity.created(URI.create("/api/empresas/" + empresaCreada.getId()))
            .body(crearRespuesta(empresaCreada, autenticacion.getName()));
    }
    @PutMapping("/{id}")
    public RespuestaEmpresa actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudEmpresa solicitud, Authentication autenticacion) {
        Empresa empresaActualizada = servicioEmpresa.actualizar(id, crearEmpresa(solicitud), autenticacion.getName());
        return crearRespuesta(empresaActualizada, autenticacion.getName());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication autenticacion) {
        servicioEmpresa.eliminar(id, autenticacion.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/servicios-post-radicacion")
    public RespuestaServiciosPostRadicacion obtenerServiciosPostRadicacion(@PathVariable Long id, Authentication autenticacion) {
        return servicioEmpresa.obtenerServiciosPostRadicacion(id, autenticacion.getName());
    }

    @PatchMapping("/{id}/servicios-post-radicacion")
    public RespuestaServiciosPostRadicacion actualizarServiciosPostRadicacion(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudServiciosPostRadicacion solicitud,
        Authentication autenticacion
    ) {
        return servicioEmpresa.actualizarServiciosPostRadicacion(id, solicitud, autenticacion.getName());
    }

    private Empresa crearEmpresa(SolicitudEmpresa solicitud) {
		return Empresa.crear(
			solicitud.nombre(),
			solicitud.razonSocial(),
			solicitud.nit(),
			solicitud.cuit(),
			solicitud.direccion(),
			solicitud.actividadEconomica(),
          solicitud.correoElectronico(),
          solicitud.telefono()
		);
    }
    private RespuestaEmpresa crearRespuesta(Empresa empresa, String identificadorIngreso) {
    boolean permiteServiciosPostRadicacion = empresa.getId() != null
      && servicioEmpresa.permiteServiciosPostRadicacion(empresa.getId(), identificadorIngreso);
        return new RespuestaEmpresa(
            empresa.getId(),
            empresa.getNombre(),
			empresa.getRazonSocial(),
			empresa.getNit(),
            empresa.getCuit(),
			empresa.getDireccion(),
			empresa.getActividadEconomica(),
			empresa.getCorreoElectronico(),
                  empresa.getTelefono(),
			empresa.getCantidadEmpleados(),
			permiteServiciosPostRadicacion
        );
    }
}