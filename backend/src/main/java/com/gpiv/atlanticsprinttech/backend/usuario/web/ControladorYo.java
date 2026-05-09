package com.gpiv.atlanticsprinttech.backend.usuario.web;

import com.gpiv.atlanticsprinttech.backend.empresa.application.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.backend.usuario.persistence.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.usuario.application.ServicioUsuario;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.SolicitudVincularEmpresa;
import com.gpiv.atlanticsprinttech.commons.shared.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.RespuestaYo;
import com.gpiv.atlanticsprinttech.commons.usuario.dto.SolicitudActualizacionPerfil;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yo")
public class ControladorYo {

    private final RepositorioUsuario repositorioUsuario;
    private final ServicioUsuario servicioUsuario;
    private final ServicioEmpresa servicioEmpresa;

    public ControladorYo(RepositorioUsuario repositorioUsuario, ServicioUsuario servicioUsuario, ServicioEmpresa servicioEmpresa) {
        this.repositorioUsuario = repositorioUsuario;
        this.servicioUsuario = servicioUsuario;
        this.servicioEmpresa = servicioEmpresa;
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

    @PatchMapping("/empresa")
    public ResponseEntity<RespuestaOperacion> vincularEmpresa(
        @Valid @RequestBody SolicitudVincularEmpresa solicitud,
        Authentication autenticacion
    ) {
        servicioEmpresa.vincularEmpresaExistente(solicitud.empresaId(), autenticacion.getName());
        return ResponseEntity.ok(new RespuestaOperacion("Empresa vinculada correctamente"));
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
}

