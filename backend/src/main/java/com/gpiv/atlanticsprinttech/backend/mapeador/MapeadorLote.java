package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad {@link Lote} a su DTO de respuesta.
 * Delega en los métodos de la entidad para cumplir Tell Don't Ask.
 */
@Component
public class MapeadorLote {

    private static final String ETIQUETA_SIN_ASIGNAR = "Sin asignar";

    public RespuestaLote aRespuesta(Lote lote) {
        String nombreEmpresa = lote.getNombreEmpresa() != null
            ? lote.getNombreEmpresa()
            : ETIQUETA_SIN_ASIGNAR;

        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.isOcupado(),
            lote.getEmpresaId(),
            nombreEmpresa,
            lote.getCuitEmpresa(),
            lote.getNombreEstadoAsignacion(),
            lote.getFechaAsignacionTexto(),
            lote.getNumeroExpedienteReferencia(),
            lote.getZona()
        );
    }
}

