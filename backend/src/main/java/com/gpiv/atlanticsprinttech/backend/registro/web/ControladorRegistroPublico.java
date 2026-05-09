package com.gpiv.atlanticsprinttech.backend.registro.web;

import com.gpiv.atlanticsprinttech.backend.usuario.application.ServicioUsuario;
import com.gpiv.atlanticsprinttech.commons.shared.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.commons.registro.dto.SolicitudRegistroPublico;
import com.gpiv.atlanticsprinttech.commons.registro.dto.SolicitudReenvioVerificacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class ControladorRegistroPublico {
    private final ServicioUsuario servicioUsuario;

    public ControladorRegistroPublico(ServicioUsuario servicioUsuario) {
        this.servicioUsuario = servicioUsuario;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaOperacion> registrar(
        @Valid @RequestBody SolicitudRegistroPublico solicitud,
        HttpServletRequest request
    ) {
        servicioUsuario.registrarPublico(
            solicitud.nombreUsuario(),
            solicitud.correoElectronico(),
            solicitud.clave(),
            solicitud.confirmacionClave(),
            obtenerIpCliente(request)
        );
        return ResponseEntity.created(URI.create("/ingreso.html"))
            .body(new RespuestaOperacion("Registro exitoso. Revisa tu correo para verificar la cuenta"));
    }

    @GetMapping("/verificacion")
    public RespuestaOperacion verificarCorreo(@RequestParam("token") String token) {
        servicioUsuario.verificarCorreoElectronico(token);
        return new RespuestaOperacion("Correo verificado con exito. Ya puedes iniciar sesion");
    }

    @PostMapping("/verificacion/reenviar")
    public RespuestaOperacion reenviarVerificacion(
        @Valid @RequestBody SolicitudReenvioVerificacion solicitud,
        HttpServletRequest request
    ) {
        servicioUsuario.reenviarCorreoVerificacion(solicitud.correoElectronico(), obtenerIpCliente(request));
        return new RespuestaOperacion("Te enviamos un nuevo correo de verificacion");
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String encabezado = request.getHeader("X-Forwarded-For");
        if (encabezado != null && !encabezado.isBlank()) {
            return encabezado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
