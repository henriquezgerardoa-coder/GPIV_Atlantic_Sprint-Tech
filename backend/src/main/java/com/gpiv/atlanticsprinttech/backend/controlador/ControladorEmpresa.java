package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorEmpresa;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaDetalleAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaListadoAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudContactoEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas")
public class ControladorEmpresa {

    private final ServicioEmpresa servicioEmpresa;
    private final MapeadorEmpresa mapeador;

    public ControladorEmpresa(ServicioEmpresa servicioEmpresa, MapeadorEmpresa mapeador) {
        this.servicioEmpresa = servicioEmpresa;
        this.mapeador = mapeador;
    }
    @GetMapping
    public List<RespuestaEmpresa> listar(Authentication autenticacion) {
        List<Empresa> empresas = servicioEmpresa.listar(autenticacion.getName());
        List<Long> ids = empresas.stream().map(Empresa::getId).toList();
        Set<Long> habilitadas = servicioEmpresa.empresasHabilitadasParaServiciosPostRadicacion(ids);
        return empresas.stream()
            .map(e -> mapeador.toRespuesta(e, habilitadas.contains(e.getId())))
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaEmpresa obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return mapeador.toRespuesta(
            servicioEmpresa.obtenerPorId(id, autenticacion.getName()),
            servicioEmpresa.permiteServiciosPostRadicacion(id, autenticacion.getName())
        );
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
        Empresa empresaCreada = servicioEmpresa.crear(mapeador.desdeSolicitud(solicitud), autenticacion.getName());
        return ResponseEntity.created(URI.create("/api/empresas/" + empresaCreada.getId()))
            .body(mapeador.toRespuesta(empresaCreada, servicioEmpresa.permiteServiciosPostRadicacion(empresaCreada.getId(), autenticacion.getName())));
    }

    @PutMapping("/{id}")
    public RespuestaEmpresa actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudEmpresa solicitud, Authentication autenticacion) {
        Empresa actualizada = servicioEmpresa.actualizar(id, mapeador.desdeSolicitud(solicitud), autenticacion.getName());
        return mapeador.toRespuesta(actualizada, servicioEmpresa.permiteServiciosPostRadicacion(id, autenticacion.getName()));
    }

    @PatchMapping("/{id}/contacto")
    public RespuestaEmpresa actualizarContacto(@PathVariable Long id, @Valid @RequestBody SolicitudContactoEmpresa solicitud, Authentication autenticacion) {
        Empresa actualizada = servicioEmpresa.actualizarContacto(id, solicitud.correoElectronico(), solicitud.telefono(), autenticacion.getName());
        return mapeador.toRespuesta(actualizada, servicioEmpresa.permiteServiciosPostRadicacion(id, autenticacion.getName()));
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

    @PatchMapping("/{id}/rubro-inicial")
    public ResponseEntity<Void> asignarRubroInicial(
        @PathVariable Long id,
        @RequestBody java.util.Map<String, Long> body,
        Authentication autenticacion
    ) {
        Long rubroId = body.get("rubroId");
        if (rubroId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "El campo rubroId es obligatorio");
        }
        servicioEmpresa.asignarRubroInicial(id, rubroId, autenticacion.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/servicios-post-radicacion")
    public RespuestaServiciosPostRadicacion actualizarServiciosPostRadicacion(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudServiciosPostRadicacion solicitud,
        Authentication autenticacion
    ) {
        return servicioEmpresa.actualizarServiciosPostRadicacion(id, solicitud, autenticacion.getName());
    }

    @GetMapping("/disponibles")
    public List<RespuestaEmpresa> listarDisponibles() {
        return servicioEmpresa.listarDisponibles().stream()
            .map(e -> mapeador.toRespuesta(e, false))
            .toList();
    }

    @PatchMapping("/{id}/inactivar")
    public ResponseEntity<Void> inactivar(@PathVariable Long id, Authentication autenticacion) {
        servicioEmpresa.inactivar(id, autenticacion.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id, Authentication autenticacion) {
        servicioEmpresa.reactivar(id, autenticacion.getName());
        return ResponseEntity.noContent().build();
    }

}