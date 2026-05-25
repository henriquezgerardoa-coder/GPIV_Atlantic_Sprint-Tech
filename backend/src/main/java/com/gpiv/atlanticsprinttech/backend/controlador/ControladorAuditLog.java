package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAuditLog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-log")
public class ControladorAuditLog {

    private final ServicioAuditLog servicioAuditLog;
    private final MapeadorAuditLog mapeador;

    public ControladorAuditLog(ServicioAuditLog servicioAuditLog, MapeadorAuditLog mapeador) {
        this.servicioAuditLog = servicioAuditLog;
        this.mapeador = mapeador;
    }

    @GetMapping
    public List<RespuestaAuditLog> listar(
        @RequestParam(required = false) String entidad,
        @RequestParam(required = false) String usuario
    ) {
        var registros = (entidad != null && !entidad.isBlank())
            ? servicioAuditLog.filtrarPorEntidad(entidad)
            : (usuario != null && !usuario.isBlank())
                ? servicioAuditLog.filtrarPorUsuario(usuario)
                : servicioAuditLog.listarTodos();

        return registros.stream().map(mapeador::aRespuesta).toList();
    }
}