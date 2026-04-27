package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaYo;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yo")
public class ControladorYo {

    private final RepositorioUsuario repositorioUsuario;

    public ControladorYo(RepositorioUsuario repositorioUsuario) {
        this.repositorioUsuario = repositorioUsuario;
    }

    @GetMapping
    public RespuestaYo obtenerUsuarioActual(Authentication autenticacion) {
        List<String> roles = autenticacion.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .toList();
        Usuario usuario = repositorioUsuario.findByNombreUsuarioConEmpresa(autenticacion.getName()).orElse(null);
        Long empresaId = usuario == null ? null : usuario.getEmpresaId();
        String nombreEmpresa = usuario != null && usuario.getEmpresa() != null ? usuario.getEmpresa().getNombre() : null;
        return new RespuestaYo(autenticacion.getName(), roles, empresaId, nombreEmpresa);
    }
}

