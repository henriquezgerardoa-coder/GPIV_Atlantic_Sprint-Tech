package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEvaluacionRadicacion;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioServicio;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEstadisticas;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaDashboardGerencial;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEstadisticas;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeLote;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaInformeServicio;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.EtapaCicloLote;
import com.gpiv.atlanticsprinttech.entities.dominio.EvaluacionRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.Servicio;
import java.util.Comparator;
import java.util.HashMap;
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
    private final RepositorioEvaluacionRadicacion repositorioEvaluacion;
    private final RepositorioProyectoProductivo repositorioProyecto;
    private final RepositorioServicio repositorioServicio;
    private final ObjectMapper objectMapper;
    private final ServicioAuditLog servicioAuditLog;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioEstadisticasImpl(
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioEvaluacionRadicacion repositorioEvaluacion,
        RepositorioProyectoProductivo repositorioProyecto,
        RepositorioServicio repositorioServicio,
        ObjectMapper objectMapper,
        ServicioAuditLog servicioAuditLog,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioEvaluacion = repositorioEvaluacion;
        this.repositorioProyecto = repositorioProyecto;
        this.repositorioServicio = repositorioServicio;
        this.objectMapper = objectMapper;
        this.servicioAuditLog = servicioAuditLog;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public RespuestaEstadisticas obtenerResumen() {
        long totalEmpresas = repositorioEmpresa.count();
        long totalLotes    = repositorioLote.countByParentLoteIsNull();
        long lotesOcupados = repositorioLote.countByOcupadoAndParentLoteIsNull(true);

        // Una sola query GROUP BY reemplaza 7 countByEtapa individuales
        Map<EtapaCicloLote, Long> porEtapa = repositorioLote.contarPorEtapa().stream()
            .collect(Collectors.toMap(
                fila -> (EtapaCicloLote) fila[0],
                fila -> (long) fila[1]
            ));
        long lotesProyectoEnEvaluacion = porEtapa.getOrDefault(EtapaCicloLote.PROYECTO_EN_EVALUACION, 0L);
        long lotesAdjudicadoPrecario   = porEtapa.getOrDefault(EtapaCicloLote.ADJUDICADO_PRECARIO, 0L);
        long lotesEnConstruccion       = porEtapa.getOrDefault(EtapaCicloLote.EN_CONSTRUCCION, 0L);
        long lotesOperativos           = porEtapa.getOrDefault(EtapaCicloLote.OPERATIVO, 0L);
        long lotesEscriturados         = porEtapa.getOrDefault(EtapaCicloLote.ESCRITURADO, 0L);
        long lotesRevertidos           = porEtapa.getOrDefault(EtapaCicloLote.REVERTIDO, 0L);

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
            lotesProyectoEnEvaluacion,
            lotesAdjudicadoPrecario,
            lotesEnConstruccion,
            lotesOperativos,
            lotesEscriturados,
            lotesRevertidos
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
        List<Empresa> empresas = repositorioEmpresa.findAllWithRubroOrderByNombre();
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
        List<Lote> lotes = repositorioLote.findAllPrincipalesConEmpresa();
        // Proyección liviana: solo numero_radicado y fecha_plazo, sin cargar empresa ni lote completo
        Map<String, java.time.LocalDate> fechaPlazoByNumero = repositorioRadicacionSolicitud
            .findNumeroRadicadoYFechaPlazo().stream()
            .filter(fila -> fila[0] != null)
            .collect(Collectors.toMap(
                fila -> (String) fila[0],
                fila -> (java.time.LocalDate) fila[1],
                (a, b) -> a
            ));
        return lotes.stream().map(l -> {
            Empresa emp = l.getEmpresa();
            java.time.LocalDate fechaPlazo = null;
            if (l.getNumeroExpedienteReferencia() != null && !l.getNumeroExpedienteReferencia().isBlank()) {
                fechaPlazo = fechaPlazoByNumero.get(l.getNumeroExpedienteReferencia());
            }
            return new RespuestaInformeLote(
                l.getId(),
                l.getCodigo(),
                l.getSuperficieMetrosCuadrados(),
                l.isOcupado(),
                l.getEstadoAsignacionLegacy(),
                l.getNombreEtapa(),
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
    public List<RespuestaInformeRadicacion> obtenerInformeRadicaciones() {
        servicioAuditLog.registrarEvento(
            servicioContextoUsuario.obtenerIdentificadorActual(), "ACCESO_INFORME", "Radicacion",
            "informe-radicaciones",
            null,
            "consulta de informe completo de radicaciones",
            UtilRed.obtenerIpActual()
        );
        List<RadicacionSolicitud> radicaciones = repositorioRadicacionSolicitud.buscarFiltrado(null, null, null, null);
        List<Long> ids = radicaciones.stream().map(RadicacionSolicitud::getId).toList();
        Map<Long, EvaluacionRadicacion> evalPorRadicacion = ids.isEmpty()
            ? Map.of()
            : repositorioEvaluacion.findByRadicacionIdIn(ids).stream()
                .collect(Collectors.toMap(e -> e.getRadicacion().getId(), e -> e));

        return radicaciones.stream().map(r -> {
            EvaluacionRadicacion ev = evalPorRadicacion.get(r.getId());
            Lote lote = r.getLote();
            return new RespuestaInformeRadicacion(
                r.getId(),
                r.getNumeroRadicado(),
                r.getEmpresa().getNombre(),
                r.getEmpresa().getCuit(),
                r.getEmpresa().getActividadEconomica(),
                r.getEstado().name(),
                r.getFechaRadicacion(),
                r.getFechaPlazo(),
                lote != null ? lote.getCodigo() : null,
                r.getTiempoEstimadoObraMeses(),
                r.getEmpleadosPrevistos(),
                ev != null ? ev.getEtapa1EmpleoDirecto() : null,
                ev != null ? ev.getEtapa1MateriaPrimaLocal() : null,
                ev != null ? ev.getEtapa1ImpactoAmbiental() : null,
                ev != null ? ev.puntuacionEtapa1() : null,
                ev != null ? ev.getEtapa1Observaciones() : null,
                ev != null && ev.etapa1Completa(),
                ev != null ? ev.getEtapa2Rentabilidad() : null,
                ev != null ? ev.getEtapa2SolidezFinanciera() : null,
                ev != null ? ev.getEtapa2InversionDeclarada() : null,
                ev != null ? ev.puntuacionEtapa2() : null,
                ev != null ? ev.getEtapa2Observaciones() : null,
                ev != null && ev.etapa2Completa(),
                ev != null ? ev.getEtapa3ViabilidadTecnica() : null,
                ev != null ? ev.getEtapa3CronogramaObra() : null,
                ev != null ? ev.getEtapa3CalidadDocumentacion() : null,
                ev != null ? ev.puntuacionEtapa3() : null,
                ev != null ? ev.getEtapa3Observaciones() : null,
                ev != null && ev.etapa3Completa(),
                ev != null ? ev.puntuacionTotal() : null,
                ev != null ? ev.getEvaluador() : null
            );
        }).toList();
    }

    @Override
    public List<RespuestaInformeServicio> obtenerInformeServicios() {
        List<Servicio> servicios = repositorioServicio.findAllConTecnico();
        List<Lote> lotesOcupados = repositorioLote.findAllPrincipalesConEmpresa().stream()
            .filter(l -> l.isOcupado() && l.getEmpresa() != null)
            .toList();

        Map<Long, Map<String, Object>> jsonPorEmpresa = new HashMap<>();
        TypeReference<Map<String, Object>> tipoMapa = new TypeReference<>() {};
        for (Lote lote : lotesOcupados) {
            Long empId = lote.getEmpresa().getId();
            if (!jsonPorEmpresa.containsKey(empId)) {
                String json = lote.getEmpresa().getServiciosPostRadicacionJson();
                try {
                    jsonPorEmpresa.put(empId, json != null ? objectMapper.readValue(json, tipoMapa) : Map.of());
                } catch (Exception e) {
                    jsonPorEmpresa.put(empId, Map.of());
                }
            }
        }

        return servicios.stream().map(servicio -> {
            String clave = _claveServicio(servicio.getNombre());
            List<RespuestaInformeServicio.EmpresaConsumidora> empresas = lotesOcupados.stream()
                .filter(l -> _usaServicio(l, clave, jsonPorEmpresa))
                .map(l -> new RespuestaInformeServicio.EmpresaConsumidora(
                    l.getEmpresa().getId(),
                    l.getEmpresa().getNombre(),
                    l.getEmpresa().getCuit(),
                    l.getId(),
                    l.getCodigo(),
                    _consumoTexto(l, clave, jsonPorEmpresa)
                ))
                .toList();
            return new RespuestaInformeServicio(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getEstadoActual().name(),
                servicio.getDescripcionTecnica(),
                empresas
            );
        }).toList();
    }

    private String _claveServicio(String nombre) {
        if (nombre == null) return "";
        String n = nombre.toLowerCase();
        if (n.contains("agua")) return "agua";
        if (n.contains("luz") || n.contains("eléctric") || n.contains("electr") || n.contains("energía")) return "luz";
        if (n.contains("gas")) return "gas";
        if (n.contains("internet") || n.contains("conectividad")) return "internet";
        return nombre.toLowerCase().trim();
    }

    private boolean _usaServicio(Lote lote, String clave, Map<Long, Map<String, Object>> jsonPorEmpresa) {
        if ("agua".equals(clave)) return true;
        Map<String, Object> datos = jsonPorEmpresa.getOrDefault(lote.getEmpresa().getId(), Map.of());
        if (datos.isEmpty()) return false;
        return switch (clave) {
            case "luz"      -> _num(datos, "consumoLuzKwh") > 0;
            case "gas"      -> _num(datos, "consumoGasM3") > 0;
            case "internet" -> _num(datos, "consumoInternetMbps") > 0;
            default -> {
                Object adicionales = datos.get("consumosAdicionales");
                if (!(adicionales instanceof List<?> lista)) yield false;
                yield lista.stream().anyMatch(item -> {
                    if (!(item instanceof Map<?, ?> m)) return false;
                    Object nom = m.get("nombre");
                    return nom instanceof String s && s.toLowerCase().contains(clave);
                });
            }
        };
    }

    private String _consumoTexto(Lote lote, String clave, Map<Long, Map<String, Object>> jsonPorEmpresa) {
        Map<String, Object> datos = jsonPorEmpresa.getOrDefault(lote.getEmpresa().getId(), Map.of());
        return switch (clave) {
            case "agua" -> {
                double val = _num(datos, "consumoAguaCrudaM3");
                yield val > 0 ? val + " m³" : "Sí";
            }
            case "luz"      -> _num(datos, "consumoLuzKwh")      + " kWh";
            case "gas"      -> _num(datos, "consumoGasM3")        + " m³";
            case "internet" -> _num(datos, "consumoInternetMbps") + " Mbps";
            default -> "—";
        };
    }

    private double _num(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number n ? n.doubleValue() : 0;
    }

    @Override
    public RespuestaDashboardGerencial obtenerDashboardGerencial() {
        List<EstadoProyecto> estadosFinalesProyecto = List.of(EstadoProyecto.COMPLETADO, EstadoProyecto.CANCELADO);
        List<EstadoRadicacion> estadosFinalesRad = List.of(
            EstadoRadicacion.RADICADA, EstadoRadicacion.RECHAZADA, EstadoRadicacion.CANCELADA
        );
        List<EstadoRadicacion> estadosConEmpleados = List.of(EstadoRadicacion.APROBADA, EstadoRadicacion.RADICADA);

        // Empresas y empleo
        List<Empresa> empresas = repositorioEmpresa.findAllWithRubroOrderByNombre();
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
        List<Lote> lotes = repositorioLote.findAllPrincipalesConEmpresa();
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
                l.getEstadoAsignacionLegacy(),
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
