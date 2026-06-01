package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MapeadorRadicacion {

    private final ObjectMapper objectMapper;
    private final RepositorioProyectoProductivo repositorioProyecto;

    public MapeadorRadicacion(ObjectMapper objectMapper, RepositorioProyectoProductivo repositorioProyecto) {
        this.objectMapper = objectMapper;
        this.repositorioProyecto = repositorioProyecto;
    }

    /** Mapeo para una lista: resuelve todos los proyectos en una sola consulta (evita N+1). */
    public List<RespuestaRadicacion> toRespuestas(List<RadicacionSolicitud> radicaciones) {
        List<Long> ids = radicaciones.stream().map(RadicacionSolicitud::getId).toList();
        Map<Long, ProyectoProductivo> proyectos = repositorioProyecto.findBySolicitudOrigenIdIn(ids).stream()
            .collect(Collectors.toMap(p -> p.getSolicitudOrigen().getId(), Function.identity()));
        return radicaciones.stream()
            .map(r -> toRespuesta(r, proyectos.get(r.getId())))
            .toList();
    }

    /** Mapeo para un único registro: consulta el proyecto individualmente. */
    public RespuestaRadicacion toRespuesta(RadicacionSolicitud radicacion) {
        ProyectoProductivo proyecto = repositorioProyecto.findBySolicitudOrigenId(radicacion.getId()).orElse(null);
        return toRespuesta(radicacion, proyecto);
    }

    public RespuestaRadicacion toRespuesta(RadicacionSolicitud radicacion, ProyectoProductivo proyecto) {
        String json = radicacion.getRelevamientoPedidoLotesJson();
        return new RespuestaRadicacion(
            radicacion.getId(),
            radicacion.getNumeroRadicado(),
            radicacion.getEmpresa().getId(),
            radicacion.getEmpresa().getNombre(),
            radicacion.getEmpresa().getRazonSocial(),
            radicacion.getEmpresa().getCuit(),
            radicacion.getEmpresa().getActividadEconomica(),
            radicacion.getEmpresa().getDireccion(),
            radicacion.getEmpresa().getCorreoElectronico(),
            radicacion.getEmpresa().getTelefono(),
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
            radicacion.obtenerIdLote(),
            radicacion.obtenerCodigoLote(),
            extraerCampoEntero(json, "necesidadMetrosCuadrados"),
            radicacion.getNumeroResolucion(),
            radicacion.getResueltoPor(),
            proyecto != null ? proyecto.getId() : null,
            proyecto != null ? proyecto.getEstado().name() : null
        );
    }

    public RespuestaLote toLoteRespuesta(Lote lote) {
        var etapa = lote.getEtapa();
        return new RespuestaLote(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.isOcupado(),
            lote.getEmpresaId(),
            lote.getNombreEmpresa(),
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
            lote.getFechaInicioEtapaActual() != null ? lote.getFechaInicioEtapaActual().toString() : null,
            lote.getFechaLimiteEtapaActual() != null ? lote.getFechaLimiteEtapaActual().toString() : null,
            lote.etapaVencida(),
            lote.diasEnEtapaActual()
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
            historial.getEstadoAnterior(),
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