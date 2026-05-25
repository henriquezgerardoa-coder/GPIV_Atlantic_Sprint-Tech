package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMonitorExpediente;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Convierte {@link RadicacionSolicitud} al DTO de monitoreo de expedientes.
 * Aplica Tell Don't Ask delegando la lógica de plazo vencido a la entidad.
 */
@Component
public class MapeadorMonitor {

    public RespuestaMonitorExpediente aRespuesta(RadicacionSolicitud radicacion) {
        long diasSinMovimiento = radicacion.getFechaUltimaActualizacion() != null
            ? ChronoUnit.DAYS.between(radicacion.getFechaUltimaActualizacion().toLocalDate(), LocalDate.now())
            : 0;

        return new RespuestaMonitorExpediente(
            radicacion.getId(),
            radicacion.getNumeroRadicado(),
            radicacion.getEmpresa().getNombre(),
            radicacion.getEmpresa().getId(),
            radicacion.getTipoSolicitud(),
            radicacion.getEstado().name(),
            radicacion.getFechaRadicacionTexto(),
            radicacion.getFechaUltimaActualizacionTexto(),
            radicacion.getFechaAprobacionTexto(),
            radicacion.getFechaPlazoTexto(),
            diasSinMovimiento,
            radicacion.esPlazoVencido()
        );
    }
}

