package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioHistorialEtapaLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioCicloLote;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAlertaVencimiento;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialEtapa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import com.gpiv.atlanticsprinttech.entities.dominio.EtapaCicloLote;
import com.gpiv.atlanticsprinttech.entities.dominio.HistorialEtapaLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioCicloLoteImpl implements ServicioCicloLote {

    private static final Logger log = LoggerFactory.getLogger(ServicioCicloLoteImpl.class);

    private final RepositorioLote repositorioLote;
    private final RepositorioHistorialEtapaLote repositorioHistorial;
    private final ServicioContextoUsuario servicioContextoUsuario;
    private final ServicioAuditLog servicioAuditLog;
    private final MapeadorLote mapeadorLote;

    @Value("${app.alertas.ciclo-lote.habilitado:true}")
    private boolean habilitado;

    public ServicioCicloLoteImpl(
        RepositorioLote repositorioLote,
        RepositorioHistorialEtapaLote repositorioHistorial,
        ServicioContextoUsuario servicioContextoUsuario,
        ServicioAuditLog servicioAuditLog,
        MapeadorLote mapeadorLote
    ) {
        this.repositorioLote = repositorioLote;
        this.repositorioHistorial = repositorioHistorial;
        this.servicioContextoUsuario = servicioContextoUsuario;
        this.servicioAuditLog = servicioAuditLog;
        this.mapeadorLote = mapeadorLote;
    }

    @Override
    @CacheEvict(value = "lotes", allEntries = true)
    public RespuestaLote transicionar(Long loteId, String etapaDestino, String motivo, String identificadorIngreso) {
        EtapaCicloLote nuevaEtapa = resolverEtapa(etapaDestino);
        Lote lote = repositorioLote.findByIdConEmpresa(loteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));

        EtapaCicloLote etapaAnterior = lote.getEtapa();

        if (!etapaAnterior.puedeTransicionarA(nuevaEtapa)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Transición no permitida: " + etapaAnterior.name() + " → " + nuevaEtapa.name() +
                ". Transiciones válidas: " + etapaAnterior.transicionesPermitidas()
            );
        }

        lote.transicionar(nuevaEtapa);

        if (nuevaEtapa == EtapaCicloLote.DISPONIBLE) {
            lote.actualizarDatos(lote.getCodigo(), lote.getSuperficieMetrosCuadrados(), false, null, lote.getZona());
        } else if (nuevaEtapa == EtapaCicloLote.REVERTIDO) {
            lote.liberar();
        }

        LocalDate fechaLimite = lote.getFechaLimiteEtapaActual();

        repositorioHistorial.save(HistorialEtapaLote.registrar(
            lote, etapaAnterior, nuevaEtapa, identificadorIngreso, motivo, fechaLimite
        ));

        repositorioLote.save(lote);

        servicioAuditLog.registrarEvento(
            identificadorIngreso, "TRANSICION_LOTE", "Lote",
            lote.getCodigo(),
            etapaAnterior.name(),
            nuevaEtapa.name(),
            UtilRed.obtenerIpActual()
        );

        return mapeadorLote.aRespuesta(repositorioLote.findByIdConEmpresa(lote.getId()).orElse(lote));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaHistorialEtapa> listarHistorial(Long loteId, String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            repositorioLote.findByIdAndEmpresaIdConEmpresa(loteId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a este lote"));
        }
        return repositorioHistorial.findByLoteIdOrdenado(loteId)
            .stream()
            .map(mapeadorLote::aRespuestaHistorial)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaAlertaVencimiento> listarAlertasVencimiento() {
        return repositorioLote.findConEtapaVencida()
            .stream()
            .map(mapeadorLote::aRespuestaAlerta)
            .toList();
    }

    @Scheduled(cron = "0 30 8 * * *")
    public void registrarAlertasVencimientoEtapa() {
        if (!habilitado) return;
        List<RespuestaAlertaVencimiento> vencidos = listarAlertasVencimiento();
        if (vencidos.isEmpty()) return;
        log.warn("[CicloLote] {} lote(s) con etapa vencida:", vencidos.size());
        for (RespuestaAlertaVencimiento a : vencidos) {
            log.warn("  → Lote {} – {}: etapa {} vencida hace {} días (límite: {})",
                a.codigoLote(), a.nombreEmpresa(), a.etapaEtiqueta(), a.diasVencida(), a.fechaLimiteEtapaActual());
        }
    }

    private EtapaCicloLote resolverEtapa(String etapa) {
        if (etapa == null || etapa.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La etapa destino es obligatoria");
        }
        try {
            return EtapaCicloLote.valueOf(etapa.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapa inválida: " + etapa);
        }
    }
}
