package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMonitorExpediente;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class ControladorMonitor {

    private final ServicioRadicacion servicioRadicacion;

    public ControladorMonitor(ServicioRadicacion servicioRadicacion) {
        this.servicioRadicacion = servicioRadicacion;
    }

    @GetMapping("/expedientes")
    public List<RespuestaMonitorExpediente> listarExpedientes(Authentication auth) {
        return servicioRadicacion.listar(auth.getName(), null, null, null).stream()
            .map(this::toMonitor)
            .toList();
    }

    private RespuestaMonitorExpediente toMonitor(RadicacionSolicitud r) {
        long diasSinMovimiento = r.getFechaUltimaActualizacion() != null
            ? ChronoUnit.DAYS.between(r.getFechaUltimaActualizacion().toLocalDate(), LocalDate.now())
            : 0;

        boolean plazoVencido = r.getFechaPlazo() != null
            && r.getFechaPlazo().isBefore(LocalDate.now())
            && !r.getEstado().esFinal();

        return new RespuestaMonitorExpediente(
            r.getId(),
            r.getNumeroRadicado(),
            r.getEmpresa().getNombre(),
            r.getEmpresa().getId(),
            r.getTipoSolicitud(),
            r.getEstado().name(),
            r.getFechaRadicacion() != null ? r.getFechaRadicacion().toString() : null,
            r.getFechaUltimaActualizacion() != null ? r.getFechaUltimaActualizacion().toString() : null,
            r.getFechaAprobacion() != null ? r.getFechaAprobacion().toString() : null,
            r.getFechaPlazo() != null ? r.getFechaPlazo().toString() : null,
            diasSinMovimiento,
            plazoVencido
        );
    }
}
