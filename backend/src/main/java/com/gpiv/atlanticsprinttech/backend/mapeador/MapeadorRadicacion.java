package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import org.springframework.stereotype.Component;

@Component
public class MapeadorRadicacion {

    private final ObjectMapper objectMapper;

    public MapeadorRadicacion(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RespuestaRadicacion toRespuesta(RadicacionSolicitud radicacion) {
        String json = radicacion.getRelevamientoPedidoLotesJson();
        Lote lote = radicacion.getLote();
        return new RespuestaRadicacion(
            radicacion.getId(),
            radicacion.getNumeroRadicado(),
            radicacion.getEmpresa().getId(),
            radicacion.getEmpresa().getNombre(),
            radicacion.getTipoSolicitud(),
            radicacion.getDescripcion(),
            radicacion.getUsoEstimativo(),
            json != null,
            radicacion.getEstado().etapa(),
            radicacion.getEstado(),
            radicacion.getFechaRadicacion(),
            radicacion.getFechaUltimaActualizacion(),
            radicacion.getTiempoEstimadoObraMeses(),
            radicacion.getFechaPlazo(),
            radicacion.getFechaAprobacion(),
            extraerCampoEntero(json, "tiempoRadicacionMeses"),
            lote != null ? lote.getId() : null,
            lote != null ? lote.getCodigo() : null,
            extraerCampoEntero(json, "necesidadMetrosCuadrados")
        );
    }

    public RespuestaLote toLoteRespuesta(Lote lote) {
        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.isOcupado(),
            lote.getEmpresaId(),
            lote.getNombreEmpresa(),
            lote.getCuitEmpresa(),
            lote.getNombreEstadoAsignacion(),
            lote.getFechaAsignacionTexto(),
            lote.getNumeroExpedienteReferencia(),
            lote.getZona()
        );
    }

    public RespuestaDocumentoRadicacion toDocumentoRespuesta(RadicacionDocumento documento) {
        return new RespuestaDocumentoRadicacion(
            documento.getId(),
            documento.getTipoDocumento(),
            documento.getNombreArchivo(),
            documento.getMimeType(),
            documento.getTamanoBytes(),
            documento.getDescripcion(),
            documento.getSubidoPor(),
            documento.getFechaSubida()
        );
    }

    public RespuestaHistorialRadicacion toHistorialRespuesta(RadicacionHistorial historial) {
        return new RespuestaHistorialRadicacion(
            historial.getId(),
            historial.getEstado(),
            historial.getComentario(),
            historial.getUsuario(),
            historial.getFechaEvento()
        );
    }

    private Integer extraerCampoEntero(String json, String campo) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode nodo = objectMapper.readTree(json);
            JsonNode valor = nodo.get(campo);
            return (valor != null && !valor.isNull()) ? valor.asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
