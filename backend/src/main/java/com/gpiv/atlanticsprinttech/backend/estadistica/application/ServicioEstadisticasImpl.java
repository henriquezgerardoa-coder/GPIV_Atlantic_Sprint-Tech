package com.gpiv.atlanticsprinttech.backend.estadistica.application;

import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.lote.persistence.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.radicacion.persistence.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.commons.estadistica.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.entities.lote.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        long lotesOcupados  = repositorioLote.countByEstadoAsignacionIsNot(EstadoAsignacionLote.DISPONIBLE);

        Map<String, Long> radicacionesPorEstado = repositorioRadicacionSolicitud.contarPorEstado()
            .stream()
            .collect(Collectors.toMap(
                fila -> ((EstadoRadicacion) fila[0]).name(),
                fila -> (long) fila[1]
            ));

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
