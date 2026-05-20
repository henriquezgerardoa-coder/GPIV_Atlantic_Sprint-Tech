package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion de estadisticas del parque industrial (R-14: lectura de informes para DIRECTIVO).
 */
@Service
@Transactional(readOnly = true)
public class ServicioEstadisticasImpl implements ServicioEstadisticas {

    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioLote repositorioLote;
    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;

    public ServicioEstadisticasImpl(
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud
    ) {
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
    }

    @Override
    public RespuestaEstadisticas obtenerResumen() {
        long totalEmpresas  = repositorioEmpresa.count();
        long totalLotes     = repositorioLote.count();
        long lotesOcupados  = repositorioLote.countByOcupado(true);

        List<Object[]> porEstado = repositorioRadicacionSolicitud.contarPorEstado();
        Map<String, Long> radicacionesPorEstado = new HashMap<>();
        for (Object[] fila : porEstado) {
            EstadoRadicacion estado = (EstadoRadicacion) fila[0];
            long conteo = (long) fila[1];
            radicacionesPorEstado.put(estado.name(), conteo);
        }

        long totalRadicaciones = radicacionesPorEstado.values().stream().mapToLong(Long::longValue).sum();
        long pendientes  = radicacionesPorEstado.getOrDefault(EstadoRadicacion.PENDIENTE.name(), 0L);
        long enRevision  = radicacionesPorEstado.getOrDefault(EstadoRadicacion.EN_REVISION.name(), 0L);
        long aprobadas   = radicacionesPorEstado.getOrDefault(EstadoRadicacion.APROBADA.name(), 0L)
            + radicacionesPorEstado.getOrDefault(EstadoRadicacion.RADICADA.name(), 0L);
        long rechazadas  = radicacionesPorEstado.getOrDefault(EstadoRadicacion.RECHAZADA.name(), 0L);

        return new RespuestaEstadisticas(
            totalEmpresas,
            totalLotes,
            lotesOcupados,
            totalLotes - lotesOcupados,
            totalRadicaciones,
            radicacionesPorEstado,
            pendientes,
            enRevision,
            aprobadas,
            rechazadas
        );
    }

    @Override
    public List<RespuestaInformeEmpresa> obtenerInformeEmpresas() {
        List<Empresa> empresas = repositorioEmpresa.findAll();
        List<RespuestaInformeEmpresa> resultado = new ArrayList<>();
        for (Empresa emp : empresas) {
            List<Lote> lotes = repositorioLote.findAllByEmpresaIdConEmpresa(emp.getId());
            Optional<RadicacionSolicitud> ultimaRad =
                repositorioRadicacionSolicitud.findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(emp.getId());

            List<RespuestaInformeEmpresa.LoteInforme> lotesInforme = lotes.stream()
                .map(l -> new RespuestaInformeEmpresa.LoteInforme(
                    l.getId(),
                    l.getCodigo(),
                    l.getSuperficieMetrosCuadrados(),
                    l.getEstadoAsignacion() != null ? l.getEstadoAsignacion().name() : null,
                    l.getZona(),
                    l.getFechaAsignacion()
                ))
                .toList();

            resultado.add(new RespuestaInformeEmpresa(
                emp.getId(),
                emp.getNombre(),
                emp.getCuit(),
                emp.getRazonSocial(),
                emp.getActividadEconomica(),
                emp.getCorreoElectronico(),
                emp.getTelefono(),
                emp.getDireccion(),
                emp.getCantidadEmpleados(),
                emp.getFechaRegistro(),
                ultimaRad.map(r -> r.getFechaRadicacion()).orElse(null),
                ultimaRad.map(r -> r.getEstado().name()).orElse(null),
                ultimaRad.map(RadicacionSolicitud::getNumeroRadicado).orElse(null),
                lotesInforme
            ));
        }
        return resultado;
    }

    @Override
    public List<RespuestaInformeLote> obtenerInformeLotes() {
        List<Lote> lotes = repositorioLote.findAllConEmpresa();
        List<RespuestaInformeLote> resultado = new ArrayList<>();
        for (Lote l : lotes) {
            Empresa emp = l.getEmpresa();
            java.time.LocalDate fechaPlazo = null;
            if (l.getNumeroExpedienteReferencia() != null && !l.getNumeroExpedienteReferencia().isBlank()) {
                Optional<RadicacionSolicitud> rad = repositorioRadicacionSolicitud
                    .buscarFiltrado(emp != null ? emp.getId() : null, null, null, null)
                    .stream()
                    .filter(r -> l.getNumeroExpedienteReferencia().equals(r.getNumeroRadicado()))
                    .findFirst();
                fechaPlazo = rad.map(RadicacionSolicitud::getFechaPlazo).orElse(null);
            }
            resultado.add(new RespuestaInformeLote(
                l.getId(),
                l.getCodigo(),
                l.getSuperficieMetrosCuadrados(),
                l.isOcupado(),
                l.getEstadoAsignacion() != null ? l.getEstadoAsignacion().name() : null,
                l.getZona(),
                emp != null ? emp.getNombre() : null,
                emp != null ? emp.getCuit() : null,
                l.getFechaAsignacion(),
                l.getNumeroExpedienteReferencia(),
                fechaPlazo
            ));
        }
        return resultado;
    }
}
