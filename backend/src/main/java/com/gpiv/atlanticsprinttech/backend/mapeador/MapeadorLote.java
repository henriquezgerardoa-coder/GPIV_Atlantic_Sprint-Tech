package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAlertaVencimiento;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialEtapa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.entities.dominio.EtapaCicloLote;
import com.gpiv.atlanticsprinttech.entities.dominio.HistorialEtapaLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class MapeadorLote {

    private static final String ETIQUETA_SIN_ASIGNAR = "Sin asignar";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RespuestaLote aRespuesta(Lote lote) {
        String nombreEmpresa = lote.getNombreEmpresa() != null
            ? lote.getNombreEmpresa()
            : ETIQUETA_SIN_ASIGNAR;

        EtapaCicloLote etapa = lote.getEtapa();

        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.isOcupado(),
            lote.getEmpresaId(),
            nombreEmpresa,
            lote.getCuitEmpresa(),
            lote.getEstadoAsignacionLegacy(),
            lote.getFechaAsignacionTexto(),
            lote.getNumeroExpedienteReferencia(),
            lote.getZona(),
            lote.getColorPersonalizado(),
            lote.getRubroEmpresa(),
            lote.getReferenteEmpresa(),
            lote.getCorreoElectronicoEmpresa(),
            lote.getTelefonoEmpresa(),
            etapa != null ? etapa.name() : null,
            etapa != null ? etapa.etiquetaLegible() : null,
            lote.getFechaInicioEtapaActual() != null ? lote.getFechaInicioEtapaActual().format(FORMATO_FECHA) : null,
            lote.getFechaLimiteEtapaActual() != null ? lote.getFechaLimiteEtapaActual().format(FORMATO_FECHA) : null,
            lote.etapaVencida(),
            lote.diasEnEtapaActual()
        );
    }

    public RespuestaHistorialEtapa aRespuestaHistorial(HistorialEtapaLote h) {
        return new RespuestaHistorialEtapa(
            h.getId(),
            h.getLote().getId(),
            h.getLote().getCodigo(),
            h.getEtapaAnterior() != null ? h.getEtapaAnterior().name() : null,
            h.getEtapaAnterior() != null ? h.getEtapaAnterior().etiquetaLegible() : null,
            h.getEtapaNueva().name(),
            h.getEtapaNueva().etiquetaLegible(),
            h.getFechaCambio().format(FORMATO_FECHA_HORA),
            h.getUsuarioResponsable(),
            h.getMotivo(),
            h.getFechaLimitePlazo() != null ? h.getFechaLimitePlazo().format(FORMATO_FECHA) : null
        );
    }

    public RespuestaAlertaVencimiento aRespuestaAlerta(Lote lote) {
        EtapaCicloLote etapa = lote.getEtapa();
        long diasVencida = lote.getFechaLimiteEtapaActual() != null
            ? java.time.temporal.ChronoUnit.DAYS.between(lote.getFechaLimiteEtapaActual(), java.time.LocalDate.now())
            : 0L;
        return new RespuestaAlertaVencimiento(
            lote.getId(),
            lote.getCodigo(),
            lote.getNombreEmpresa(),
            etapa != null ? etapa.name() : null,
            etapa != null ? etapa.etiquetaLegible() : null,
            lote.getFechaLimiteEtapaActual() != null ? lote.getFechaLimiteEtapaActual().format(FORMATO_FECHA) : null,
            diasVencida
        );
    }
}
