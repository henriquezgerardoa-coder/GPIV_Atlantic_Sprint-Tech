package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaYo;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudActualizacionPerfil;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yo")
public class ControladorYo {

    private final RepositorioUsuario repositorioUsuario;
    private final ServicioUsuario servicioUsuario;

    public ControladorYo(RepositorioUsuario repositorioUsuario, ServicioUsuario servicioUsuario) {
        this.repositorioUsuario = repositorioUsuario;
        this.servicioUsuario = servicioUsuario;
    }

    @GetMapping
    public RespuestaYo obtenerUsuarioActual(Authentication autenticacion) {
        List<String> roles = autenticacion.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .toList();
        Usuario usuario = repositorioUsuario.findByNombreUsuarioConEmpresa(autenticacion.getName()).orElse(null);
        Long empresaId = usuario == null ? null : usuario.getEmpresaId();
        String nombreEmpresa = usuario != null && usuario.getEmpresa() != null ? usuario.getEmpresa().getNombre() : null;
        String nombreCompleto = usuario == null ? null : usuario.getNombreCompleto();
        String correoElectronico = usuario == null ? null : usuario.getCorreoElectronico();
        return new RespuestaYo(autenticacion.getName(), nombreCompleto, correoElectronico, roles, empresaId, nombreEmpresa);
    }

    @PatchMapping("/perfil")
    public ResponseEntity<RespuestaOperacion> actualizarPerfil(
        @Valid @RequestBody SolicitudActualizacionPerfil solicitud,
        Authentication autenticacion
    ) {
        servicioUsuario.actualizarPerfilPropio(
            autenticacion.getName(),
            solicitud.nombreCompleto(),
            solicitud.correoElectronico()
        );
        return ResponseEntity.ok(new RespuestaOperacion("Datos personales actualizados"));
    }

    @PostMapping("/vincular-empresa")
    public ResponseEntity<RespuestaOperacion> vincularEmpresa(
        @Valid @RequestBody SolicitudVinculacionEmpresa solicitud,
        Authentication autenticacion
    ) {
        servicioUsuario.vincularUsuarioEmpresa(autenticacion.getName(), solicitud.empresaId());
        return ResponseEntity.ok(new RespuestaOperacion("Vinculación exitosa con la empresa"));
    }

    public record SolicitudVinculacionEmpresa(Long empresaId) {}
}


