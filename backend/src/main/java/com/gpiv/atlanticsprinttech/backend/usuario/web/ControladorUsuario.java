package com.gpiv.atlanticsprinttech.backend.usuario.web;

import com.gpiv.atlanticsprinttech.backend.config.PropiedadesApiUsuarios;
import com.gpiv.atlanticsprinttech.backend.usuario.application.ServicioUsuario;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.RespuestaUsuario;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.SolicitudActualizacionUsuario;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.SolicitudCambioClave;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.SolicitudRestablecerClave;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.SolicitudUsuario;
import com.gpiv.atlanticsprinttech.entities.usuario.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
@RequestMapping("/api/usuarios")
public class ControladorUsuario {
    private final ServicioUsuario servicioUsuario;
    private final PropiedadesApiUsuarios propiedadesApiUsuarios;

    public ControladorUsuario(
        ServicioUsuario servicioUsuario,
        PropiedadesApiUsuarios propiedadesApiUsuarios
    ) {
        this.servicioUsuario = servicioUsuario;
        this.propiedadesApiUsuarios = propiedadesApiUsuarios;
    }
    @GetMapping
    public List<RespuestaUsuario> listar() {
        return servicioUsuario.listar().stream().map(this::crearRespuesta).toList();
    }
    @GetMapping("/roles")
    public ResponseEntity<List<RolUsuario>> listarRolesDisponiblesLegado() {
        return ResponseEntity.ok()
            .header("Deprecation", propiedadesApiUsuarios.getRolesLegado().getDeprecation())
            .header("Sunset", propiedadesApiUsuarios.getRolesLegado().getSunset())
            .header("Link", propiedadesApiUsuarios.getRolesLegado().getLink())
            .body(Arrays.stream(RolUsuario.values()).toList());
    }
    @GetMapping("/{id}")
    public RespuestaUsuario obtenerPorId(@PathVariable Long id) {
        return crearRespuesta(servicioUsuario.obtenerPorId(id));
    }
    @PostMapping
    public ResponseEntity<RespuestaUsuario> crear(@Valid @RequestBody SolicitudUsuario solicitud) {
        Usuario usuarioCreado = servicioUsuario.crear(
            solicitud.nombreUsuario(),
            solicitud.nombreCompleto(),
            solicitud.clave(),
            solicitud.activo(),
            solicitud.roles(),
            solicitud.empresaId()
        );
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuarioCreado.getId()))
            .body(crearRespuesta(usuarioCreado));
    }
    @PutMapping("/{id}")
    public RespuestaUsuario actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudActualizacionUsuario solicitud) {
        Usuario usuarioActualizado = servicioUsuario.actualizar(
            id,
            solicitud.nombreCompleto(),
            solicitud.activo(),
            solicitud.roles(),
            solicitud.empresaId()
        );
        return crearRespuesta(usuarioActualizado);
    }
    @PatchMapping("/{id}/clave")
    public ResponseEntity<Void> restablecerClave(@PathVariable Long id, @Valid @RequestBody SolicitudRestablecerClave solicitud) {
        servicioUsuario.restablecerClave(id, solicitud.claveNueva());
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/mi-clave")
    public ResponseEntity<Void> cambiarMiClave(@Valid @RequestBody SolicitudCambioClave solicitud, Authentication autenticacion) {
        servicioUsuario.cambiarClavePropia(autenticacion.getName(), solicitud.claveActual(), solicitud.claveNueva());
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioUsuario.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    private RespuestaUsuario crearRespuesta(Usuario usuario) {
        return new RespuestaUsuario(
            usuario.getId(),
            usuario.getNombreUsuario(),
            usuario.getNombreCompleto(),
            usuario.isActivo(),
            usuario.getRoles(),
            usuario.getEmpresaId(),
            null
        );
    }
}