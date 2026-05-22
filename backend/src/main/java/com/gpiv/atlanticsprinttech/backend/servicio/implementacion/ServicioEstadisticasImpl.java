package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// R-14: lectura de informes para DIRECTIVO. El acceso a informes completos queda registrado en audit_log.
@Service
@Transactional(readOnly = true)
public class ServicioEstadisticasImpl implements ServicioEstadisticas {

    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioLote repositorioLote;
    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
    private final RepositorioProyectoProductivo repositorioProyecto;
    private final ServicioAuditLog servicioAuditLog;

    public ServicioEstadisticasImpl(
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioProyectoProductivo repositorioProyecto,
        ServicioAuditLog servicioAuditLog
    ) {
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioProyecto = repositorioProyecto;
        this.servicioAuditLog = servicioAuditLog;
    }

    @Override
    public RespuestaEstadisticas obtenerResumen() {
        long totalEmpresas       = repositorioEmpresa.count();
        long totalLotes          = repositorioLote.count();
        long lotesOcupados       = repositorioLote.countByOcupado(true);
        long lotesPreadjudicados = repositorioLote.countByEstadoAsignacion(EstadoAsignacionLote.PREADJUDICADO);
        long lotesAdjudicados    = repositorioLote.countByEstadoAsignacion(EstadoAsignacionLote.ADJUDICADO);
        long lotesDesadjudicados = repositorioLote.countByEstadoAsignacion(EstadoAsignacionLote.DESADJUDICADO);

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
            rechazadas,
            lotesPreadjudicados,
            lotesAdjudicados,
            lotesDesadjudicados
        );
    }

    @Override
    public List<RespuestaInformeEmpresa> obtenerInformeEmpresas() {
        servicioAuditLog.registrarEvento(
            obtenerUsuarioActual(), "ACCESO_INFORME", "Empresa",
            "informe-empresas",
            null,
            "consulta de informe completo de empresas",
            obtenerIpActual()
        );
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
        servicioAuditLog.registrarEvento(
            obtenerUsuarioActual(), "ACCESO_INFORME", "Lote",
            "informe-lotes",
            null,
            "consulta de informe completo de lotes",
            obtenerIpActual()
        );
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

    @Override
    public RespuestaDashboardGerencial obtenerDashboardGerencial() {
        List<EstadoProyecto> estadosFinalesProyecto = List.of(EstadoProyecto.FINALIZADO, EstadoProyecto.CANCELADO);
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
        Map<String, Long> conteoPorRubro = new LinkedHashMap<>();
        for (Empresa e : empresas) {
            String nombreRubro = e.getRubro() != null ? e.getRubro().getNombre() : "Sin rubro";
            conteoPorRubro.merge(nombreRubro, 1L, Long::sum);
        }
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

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
    }

    private String obtenerIpActual() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "desconocida";
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();
    }
}
