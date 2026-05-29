package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDashboardGerencial;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lectura de informes para DIRECTIVO. El acceso queda registrado en audit_log.
@Service
@Transactional(readOnly = true)
public class ServicioEstadisticasImpl implements ServicioEstadisticas {

    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioLote repositorioLote;
    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
    private final RepositorioProyectoProductivo repositorioProyecto;
    private final ServicioAuditLog servicioAuditLog;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioEstadisticasImpl(
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioProyectoProductivo repositorioProyecto,
        ServicioAuditLog servicioAuditLog,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioProyecto = repositorioProyecto;
        this.servicioAuditLog = servicioAuditLog;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public RespuestaEstadisticas obtenerResumen() {
        long totalEmpresas       = repositorioEmpresa.count();
        long totalLotes          = repositorioLote.count();
        long lotesOcupados       = repositorioLote.countByOcupado(true);
        long lotesPreadjudicados = repositorioLote.countByEstadoAsignacion(EstadoAsignacionLote.PREADJUDICADO);
        long lotesAdjudicados    = repositorioLote.countByEstadoAsignacion(EstadoAsignacionLote.ADJUDICADO);
        long lotesDesadjudicados = repositorioLote.countByEstadoAsignacion(EstadoAsignacionLote.DESADJUDICADO);

        Map<String, Long> radicacionesPorEstado = repositorioRadicacionSolicitud.contarPorEstado().stream()
            .collect(Collectors.toMap(
                fila -> ((EstadoRadicacion) fila[0]).name(),
                fila -> (long) fila[1]
            ));

        long totalRadicaciones = radicacionesPorEstado.values().stream().mapToLong(Long::longValue).sum();
        long pendientes  = radicacionesPorEstado.getOrDefault(EstadoRadicacion.PENDIENTE.name(), 0L);
        long enRevision  = radicacionesPorEstado.getOrDefault(EstadoRadicacion.EN_REVISION.name(), 0L);
        long aprobadas   = radicacionesPorEstado.getOrDefault(EstadoRadicacion.APROBADA.name(), 0L)
            + radicacionesPorEstado.getOrDefault(EstadoRadicacion.RADICADA.name(), 0L);
        long rechazadas  = radicacionesPorEstado.getOrDefault(EstadoRadicacion.RECHAZADA.name(), 0L)
            + radicacionesPorEstado.getOrDefault(EstadoRadicacion.CANCELADA.name(), 0L)
            + radicacionesPorEstado.getOrDefault(EstadoRadicacion.DESADJUDICACION.name(), 0L);

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
            rechazadas,
            lotesPreadjudicados,
            lotesAdjudicados,
            lotesDesadjudicados
        );
    }

    @Override
    public List<RespuestaInformeEmpresa> obtenerInformeEmpresas() {
        servicioAuditLog.registrarEvento(
            servicioContextoUsuario.obtenerIdentificadorActual(), "ACCESO_INFORME", "Empresa",
            "informe-empresas",
            null,
            "consulta de informe completo de empresas",
            UtilRed.obtenerIpActual()
        );
        List<Empresa> empresas = repositorioEmpresa.findAll();
        return empresas.stream().map(this::aInformeEmpresa).toList();
    }

    @Override
    public List<RespuestaInformeLote> obtenerInformeLotes() {
        servicioAuditLog.registrarEvento(
            servicioContextoUsuario.obtenerIdentificadorActual(), "ACCESO_INFORME", "Lote",
            "informe-lotes",
            null,
            "consulta de informe completo de lotes",
            UtilRed.obtenerIpActual()
        );
        List<Lote> lotes = repositorioLote.findAllConEmpresa();
        Map<String, RadicacionSolicitud> radPorNumero = repositorioRadicacionSolicitud
            .buscarFiltrado(null, null, null, null).stream()
            .filter(r -> r.getNumeroRadicado() != null)
            .collect(Collectors.toMap(RadicacionSolicitud::getNumeroRadicado, r -> r, (a, b) -> a));
        return lotes.stream().map(l -> {
            Empresa emp = l.getEmpresa();
            java.time.LocalDate fechaPlazo = null;
            if (l.getNumeroExpedienteReferencia() != null && !l.getNumeroExpedienteReferencia().isBlank()) {
                fechaPlazo = Optional.ofNullable(radPorNumero.get(l.getNumeroExpedienteReferencia()))
                    .map(RadicacionSolicitud::getFechaPlazo).orElse(null);
            }
            return new RespuestaInformeLote(
                l.getId(),
                l.getCodigo(),
                l.getSuperficieMetrosCuadrados(),
                l.isOcupado(),
                Optional.ofNullable(l.getEstadoAsignacion()).map(Enum::name).orElse(null),
                l.getZona(),
                emp != null ? emp.getNombre() : null,
                emp != null ? emp.getCuit() : null,
                l.getFechaAsignacion(),
                l.getNumeroExpedienteReferencia(),
                fechaPlazo
            );
        }).toList();
    }

    @Override
    public RespuestaDashboardGerencial obtenerDashboardGerencial() {
        List<EstadoProyecto> estadosFinalesProyecto = List.of(EstadoProyecto.COMPLETADO, EstadoProyecto.CANCELADO);
        List<EstadoRadicacion> estadosFinalesRad = List.of(
            EstadoRadicacion.RADICADA, EstadoRadicacion.RECHAZADA, EstadoRadicacion.CANCELADA
        );
        List<EstadoRadicacion> estadosConEmpleados = List.of(EstadoRadicacion.APROBADA, EstadoRadicacion.RADICADA);

        // Empresas y empleo
        List<Empresa> empresas = repositorioEmpresa.findAll();
        long totalEmpresas = empresas.size();
        long empleosActuales = empresas.stream()
            .filter(e -> e.getCantidadEmpleados() != null)
            .mapToLong(Empresa::getCantidadEmpleados)
            .sum();

        // Distribución por rubro
        Map<String, Long> conteoPorRubro = empresas.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getRubro()).map(r -> r.getNombre()).orElse("Sin rubro"),
                LinkedHashMap::new,
                Collectors.counting()
            ));
        List<RespuestaDashboardGerencial.ItemRubro> distribucionRubro = conteoPorRubro.entrySet().stream()
            .sorted(Comparator.comparingLong(Map.Entry<String, Long>::getValue).reversed())
            .map(entry -> new RespuestaDashboardGerencial.ItemRubro(entry.getKey(), entry.getValue()))
            .toList();

        // Lotes y zonas
        List<Lote> lotes = repositorioLote.findAllConEmpresa();
        long totalLotes = lotes.size();
        long lotesOcupados = lotes.stream().filter(Lote::isOcupado).count();
        double superficieTotalM2 = lotes.stream()
            .mapToDouble(l -> l.getSuperficieMetrosCuadrados() != null ? l.getSuperficieMetrosCuadrados() : 0.0)
            .sum();
        double superficieOcupadaM2 = lotes.stream()
            .filter(Lote::isOcupado)
            .mapToDouble(l -> l.getSuperficieMetrosCuadrados() != null ? l.getSuperficieMetrosCuadrados() : 0.0)
            .sum();
        double tasaOcupacion = totalLotes > 0 ? (double) lotesOcupados / totalLotes * 100.0 : 0.0;

        // Distribución por zona
        Map<String, List<Lote>> lotesPorZona = lotes.stream()
            .collect(Collectors.groupingBy(l -> l.getZona() != null ? l.getZona() : "Sin zona"));
        List<RespuestaDashboardGerencial.ItemZona> distribucionZona = lotesPorZona.entrySet().stream()
            .map(entry -> {
                List<Lote> grupo = entry.getValue();
                long ocupadosZona = grupo.stream().filter(Lote::isOcupado).count();
                double supZona = grupo.stream()
                    .mapToDouble(l -> l.getSuperficieMetrosCuadrados() != null ? l.getSuperficieMetrosCuadrados() : 0.0)
                    .sum();
                double tasa = grupo.isEmpty() ? 0 : (double) ocupadosZona / grupo.size() * 100.0;
                return new RespuestaDashboardGerencial.ItemZona(
                    entry.getKey(), grupo.size(), ocupadosZona, grupo.size() - ocupadosZona, supZona, tasa
                );
            })
            .sorted(Comparator.comparing(RespuestaDashboardGerencial.ItemZona::zona))
            .toList();

        // Empleo proyectado y radicaciones activas
        Long empleosProyectados = repositorioRadicacionSolicitud.sumEmpleadosPrevistos(estadosConEmpleados);
        long radicacionesActivas = repositorioRadicacionSolicitud.countActivas(estadosFinalesRad);

        // Proyectos
        long proyectosActivos = repositorioProyecto.countByEstadoNotIn(estadosFinalesProyecto);
        long proyectosConHitosVencidos = repositorioProyecto.countConHitosVencidos(estadosFinalesProyecto);

        return new RespuestaDashboardGerencial(
            totalEmpresas,
            totalLotes,
            lotesOcupados,
            totalLotes - lotesOcupados,
            Math.round(tasaOcupacion * 10.0) / 10.0,
            Math.round(superficieTotalM2 * 10.0) / 10.0,
            Math.round(superficieOcupadaM2 * 10.0) / 10.0,
            empleosActuales,
            empleosProyectados != null ? empleosProyectados : 0L,
            radicacionesActivas,
            proyectosActivos,
            proyectosConHitosVencidos,
            distribucionRubro,
            distribucionZona
        );
    }

    private RespuestaInformeEmpresa aInformeEmpresa(Empresa emp) {
        List<Lote> lotes = repositorioLote.findAllByEmpresaIdConEmpresa(emp.getId());
        Optional<RadicacionSolicitud> ultimaRad =
            repositorioRadicacionSolicitud.findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(emp.getId());
        List<RespuestaInformeEmpresa.LoteInforme> lotesInforme = lotes.stream()
            .map(l -> new RespuestaInformeEmpresa.LoteInforme(
                l.getId(), l.getCodigo(), l.getSuperficieMetrosCuadrados(),
                Optional.ofNullable(l.getEstadoAsignacion()).map(Enum::name).orElse(null),
                l.getZona(), l.getFechaAsignacion()
            ))
            .toList();
        return new RespuestaInformeEmpresa(
            emp.getId(), emp.getNombre(), emp.getCuit(), emp.getRazonSocial(),
            emp.getActividadEconomica(), emp.getCorreoElectronico(), emp.getTelefono(), emp.getDireccion(),
            emp.getCantidadEmpleados(), emp.getFechaRegistro(),
            ultimaRad.map(RadicacionSolicitud::getFechaRadicacion).orElse(null),
            ultimaRad.map(RadicacionSolicitud::getEstado).map(Enum::name).orElse(null),
            ultimaRad.map(RadicacionSolicitud::getNumeroRadicado).orElse(null),
            lotesInforme
        );
    }
}
