package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaEnLoteCompartido;
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
    private final RepositorioRadicacionSolicitud repositorioRadicacion;

    public MapeadorRadicacion(ObjectMapper objectMapper, RepositorioProyectoProductivo repositorioProyecto,
                              RepositorioRadicacionSolicitud repositorioRadicacion) {
        this.objectMapper = objectMapper;
        this.repositorioProyecto = repositorioProyecto;
        this.repositorioRadicacion = repositorioRadicacion;
    }

    /** Mapeo para una lista: resuelve proyectos en una sola consulta; sin compartidos (evita N+1). */
    public List<RespuestaRadicacion> toRespuestas(List<RadicacionSolicitud> radicaciones) {
        List<Long> ids = radicaciones.stream().map(RadicacionSolicitud::getId).toList();
        Map<Long, ProyectoProductivo> proyectos = repositorioProyecto.findBySolicitudOrigenIdIn(ids).stream()
            .collect(Collectors.toMap(p -> p.getSolicitudOrigen().getId(), Function.identity()));
        return radicaciones.stream()
            .map(r -> toRespuesta(r, proyectos.get(r.getId()), List.of()))
            .toList();
    }

    /** Mapeo para un único registro: consulta proyecto y coradicaciones del mismo lote. */
    public RespuestaRadicacion toRespuesta(RadicacionSolicitud radicacion) {
        ProyectoProductivo proyecto = repositorioProyecto.findBySolicitudOrigenId(radicacion.getId()).orElse(null);
        Long loteId = radicacion.obtenerIdLote();
        List<RespuestaEmpresaEnLoteCompartido> compartidos = loteId != null
            ? repositorioRadicacion.findByLoteIdExcluyendo(loteId, radicacion.getId()).stream()
                .map(r -> new RespuestaEmpresaEnLoteCompartido(
                    r.getId(),
                    r.getNumeroRadicado(),
                    r.getEmpresa().getId(),
                    r.getEmpresa().getNombre(),
                    r.getEstado().name()
                ))
                .toList()
            : List.of();
        return toRespuesta(radicacion, proyecto, compartidos);
    }

    public RespuestaRadicacion toRespuesta(RadicacionSolicitud radicacion, ProyectoProductivo proyecto,
                                           List<RespuestaEmpresaEnLoteCompartido> otrasEmpresas) {
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
            proyecto != null ? proyecto.getEstado().name() : null,
            otrasEmpresas
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