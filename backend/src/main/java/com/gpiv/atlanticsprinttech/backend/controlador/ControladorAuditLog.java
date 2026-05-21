package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAuditLog;
import com.gpiv.atlanticsprinttech.entities.dominio.AuditLog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-log")
public class ControladorAuditLog {

    private final ServicioAuditLog servicioAuditLog;

    public ControladorAuditLog(ServicioAuditLog servicioAuditLog) {
        this.servicioAuditLog = servicioAuditLog;
    }

    @GetMapping
    public List<RespuestaAuditLog> listar(
        @RequestParam(required = false) String entidad,
        @RequestParam(required = false) String usuario
    ) {
        List<AuditLog> logs;
        if (entidad != null && !entidad.isBlank()) {
            logs = servicioAuditLog.filtrarPorEntidad(entidad);
        } else if (usuario != null && !usuario.isBlank()) {
            logs = servicioAuditLog.filtrarPorUsuario(usuario);
        } else {
            logs = servicioAuditLog.listarTodos();
        }
        return logs.stream().map(this::mapear).toList();
    }

    private RespuestaAuditLog mapear(AuditLog log) {
        return new RespuestaAuditLog(
            log.getId(),
            log.getFechaHora(),
            log.getUsuarioResponsable(),
            log.getOperacion(),
            log.getEntidadAfectada(),
            log.getIdReferencia(),
            log.getValorAnterior(),
            log.getValorNuevo(),
            log.getDireccionIp()
        );
    }
}