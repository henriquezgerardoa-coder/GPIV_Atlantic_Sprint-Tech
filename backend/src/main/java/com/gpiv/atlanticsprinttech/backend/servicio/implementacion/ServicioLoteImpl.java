package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaReservaLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioLoteImpl implements ServicioLote {

    private final RepositorioLote repositorioLote;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioRadicacionSolicitud repositorioRadicacion;
    private final ServicioContextoUsuario servicioContextoUsuario;
    private final ServicioAuditLog servicioAuditLog;

    public ServicioLoteImpl(
        RepositorioLote repositorioLote,
        RepositorioEmpresa repositorioEmpresa,
        RepositorioRadicacionSolicitud repositorioRadicacion,
        ServicioContextoUsuario servicioContextoUsuario,
        ServicioAuditLog servicioAuditLog
    ) {
        this.repositorioLote = repositorioLote;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioRadicacion = repositorioRadicacion;
        this.servicioContextoUsuario = servicioContextoUsuario;
        this.servicioAuditLog = servicioAuditLog;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listar(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioLote.findAllByEmpresaIdConEmpresa(empresaId);
        }
        return repositorioLote.findAllConEmpresa();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarDisponibles() {
        return repositorioLote.findAllDisponiblesConEmpresa();
    }

    @Override
    public Lote obtenerPorId(Long id, String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioLote.findByIdAndEmpresaIdConEmpresa(id, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
        }
        return repositorioLote.findByIdConEmpresa(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
    }

    @Override
    @CacheEvict(value = "lotes", allEntries = true)
    public Lote crear(
        String codigo,
        Double superficieMetrosCuadrados,
        boolean ocupado,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String zona,
        String identificadorIngreso
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaObjetivo = empresaId;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            empresaObjetivo = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        }
        Empresa empresa = obtenerEmpresaOpcional(empresaObjetivo);
        validarCodigoDisponible(empresaObjetivo, codigo, null);
        Lote lote = Lote.crear(codigo, superficieMetrosCuadrados, ocupado, empresa, normalizarTexto(zona));
        lote.setExternalId(null);
        Lote guardado = repositorioLote.save(lote);
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "CREACION_LOTE", "Lote",
            guardado.getCodigo(),
            null,
            guardado.getCodigo() + " | " + guardado.getSuperficieMetrosCuadrados() + "m2 | ocupado=" + guardado.isOcupado(),
            UtilRed.obtenerIpActual()
        );
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    @CacheEvict(value = "lotes", allEntries = true)
    public Lote actualizar(
        Long id,
        String codigo,
        Double superficieMetrosCuadrados,
        boolean ocupado,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String zona,
        String identificadorIngreso
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Lote loteActual = obtenerPorId(id, identificadorIngreso);
        String anteriorLote = loteActual.getCodigo() + " | " + loteActual.getSuperficieMetrosCuadrados() + "m2 | ocupado=" + loteActual.isOcupado();
        Long empresaObjetivo = empresaId;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            empresaObjetivo = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        }
        Empresa empresa = obtenerEmpresaOpcional(empresaObjetivo);
        validarCodigoDisponible(empresaObjetivo, codigo, loteActual);
        loteActual.actualizarDatos(codigo, superficieMetrosCuadrados, ocupado, empresa, normalizarTexto(zona));
        Lote guardado = repositorioLote.save(loteActual);
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "ACTUALIZACION_LOTE", "Lote",
            guardado.getCodigo(),
            anteriorLote,
            guardado.getCodigo() + " | " + guardado.getSuperficieMetrosCuadrados() + "m2 | ocupado=" + guardado.isOcupado(),
            UtilRed.obtenerIpActual()
        );
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    @CacheEvict(value = "lotes", allEntries = true)
    public void eliminar(Long id, String identificadorIngreso) {
        Lote loteActual = obtenerPorId(id, identificadorIngreso);
        String codigoLote = loteActual.getCodigo();
        String datosLote = codigoLote + " | " + loteActual.getSuperficieMetrosCuadrados() + "m2 | ocupado=" + loteActual.isOcupado();
        repositorioLote.delete(loteActual);
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "ELIMINACION_LOTE", "Lote",
            codigoLote,
            datosLote,
            null,
            UtilRed.obtenerIpActual()
        );
    }

    @Override
    @CacheEvict(value = "lotes", allEntries = true)
    public Lote actualizarColor(Long id, String color, String identificadorIngreso) {
        Lote lote = obtenerPorId(id, identificadorIngreso);
        lote.asignarColor(color);
        return repositorioLote.findByIdConEmpresa(repositorioLote.save(lote).getId()).orElse(lote);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaReservaLote> listarReservas(Long loteId) {
        return repositorioRadicacion.findByLoteIdConEmpresa(loteId).stream()
            .map(r -> new RespuestaReservaLote(
                r.getId(),
                r.getNumeroRadicado(),
                r.getEmpresa().getId(),
                r.getEmpresa().getNombre(),
                r.getEstado().name(),
                r.getFechaRadicacion() != null ? r.getFechaRadicacion().toString() : null
            ))
            .toList();
    }

    private Empresa obtenerEmpresa(Long empresaId) {
        return repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa indicada no existe"));
    }

    private Empresa obtenerEmpresaOpcional(Long empresaId) {
        if (empresaId == null) {
            return null;
        }
        return obtenerEmpresa(empresaId);
    }

    private void validarCodigoDisponible(Long empresaId, String codigoNuevo, Lote loteActual) {
        if (empresaId == null) {
            if (loteActual == null) {
                if (repositorioLote.existsByEmpresaIsNullAndCodigo(codigoNuevo)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote sin asignar con ese codigo");
                }
                return;
            }

            boolean cambioEmpresa = loteActual.tieneEmpresaAsignada();
            boolean cambioCodigo = !loteActual.getCodigo().equals(codigoNuevo);
            if ((cambioEmpresa || cambioCodigo) && repositorioLote.existsByEmpresaIsNullAndCodigo(codigoNuevo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote sin asignar con ese codigo");
            }
            return;
        }

        if (loteActual == null) {
            if (repositorioLote.existsByEmpresa_IdAndCodigo(empresaId, codigoNuevo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
            }
            return;
        }

        boolean cambioEmpresa = loteActual.cambioDeEmpresa(empresaId);
        boolean cambioCodigo = !loteActual.getCodigo().equals(codigoNuevo);
        if ((cambioEmpresa || cambioCodigo) && repositorioLote.existsByEmpresa_IdAndCodigo(empresaId, codigoNuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
