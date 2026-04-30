package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaYo;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yo")
public class ControladorYo {

    @GetMapping
    public RespuestaYo obtenerUsuarioActual(Authentication autenticacion) {
        List<String> roles = autenticacion.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .toList();
        return new RespuestaYo(autenticacion.getName(), roles);
    }
}

