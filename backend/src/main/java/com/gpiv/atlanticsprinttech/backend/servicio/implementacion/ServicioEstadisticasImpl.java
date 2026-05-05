package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
}
