package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorMonitor;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMonitorExpediente;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class ControladorMonitor {

    private final ServicioRadicacion servicioRadicacion;
    private final MapeadorMonitor mapeador;

    public ControladorMonitor(ServicioRadicacion servicioRadicacion, MapeadorMonitor mapeador) {
        this.servicioRadicacion = servicioRadicacion;
        this.mapeador = mapeador;
    }

    @GetMapping("/expedientes")
    public List<RespuestaMonitorExpediente> listarExpedientes(Authentication auth) {
        return servicioRadicacion.listar(auth.getName(), null, null, null).stream()
            .map(mapeador::aRespuesta)
            .toList();
    }
}
