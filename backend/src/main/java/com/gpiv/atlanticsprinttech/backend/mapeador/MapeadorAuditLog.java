package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAuditLog;
import com.gpiv.atlanticsprinttech.entities.dominio.AuditLog;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad {@link AuditLog} a su DTO de respuesta.
 */
@Component
public class MapeadorAuditLog {

    public RespuestaAuditLog aRespuesta(AuditLog registro) {
        return new RespuestaAuditLog(
            registro.getId(),
            registro.getFechaHora(),
            registro.getUsuarioResponsable(),
            registro.getOperacion(),
            registro.getEntidadAfectada(),
            registro.getIdReferencia(),
            registro.getValorAnterior(),
            registro.getValorNuevo(),
            registro.getDireccionIp()
        );
    }
}

