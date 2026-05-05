package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLote;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioLoteImpl implements ServicioLote {

    private final RepositorioLote repositorioLote;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioLoteImpl(
        RepositorioLote repositorioLote,
        RepositorioEmpresa repositorioEmpresa,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioLote = repositorioLote;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public List<Lote> listar(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioLote.findAllByEmpresaIdConEmpresa(empresaId);
        }
        return repositorioLote.findAllConEmpresa();
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
    public Lote crear(
        String codigo,
        Double superficieMetrosCuadrados,
        boolean ocupado,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String identificadorIngreso
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaObjetivo = empresaId;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            empresaObjetivo = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        }
        Empresa empresa = obtenerEmpresaOpcional(empresaObjetivo);
        validarCodigoDisponible(empresaObjetivo, codigo, null);
        Lote lote = Lote.crear(codigo, superficieMetrosCuadrados, ocupado, empresa);
        lote.actualizarAsignacion(resolverEstadoAsignacion(estadoAsignacion), normalizarTexto(numeroExpedienteReferencia));
        Lote guardado = repositorioLote.save(lote);
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    public Lote actualizar(
        Long id,
        String codigo,
        Double superficieMetrosCuadrados,
        boolean ocupado,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String identificadorIngreso
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Lote loteActual = obtenerPorId(id, identificadorIngreso);
        Long empresaObjetivo = empresaId;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            empresaObjetivo = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        }
        Empresa empresa = obtenerEmpresaOpcional(empresaObjetivo);
        validarCodigoDisponible(empresaObjetivo, codigo, loteActual);
        loteActual.actualizarDatos(codigo, superficieMetrosCuadrados, ocupado, empresa);
        loteActual.actualizarAsignacion(resolverEstadoAsignacion(estadoAsignacion), normalizarTexto(numeroExpedienteReferencia));
        Lote guardado = repositorioLote.save(loteActual);
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    public void eliminar(Long id, String identificadorIngreso) {
        Lote loteActual = obtenerPorId(id, identificadorIngreso);
        repositorioLote.delete(loteActual);
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

            Long empresaActualId = loteActual.getEmpresa() != null ? loteActual.getEmpresa().getId() : null;
            boolean cambioEmpresa = empresaActualId != null;
            boolean cambioCodigo = !loteActual.getCodigo().equals(codigoNuevo);
            if ((cambioEmpresa || cambioCodigo) && repositorioLote.existsByEmpresaIsNullAndCodigo(codigoNuevo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote sin asignar con ese codigo");
            }
            return;
        }

        if (loteActual == null) {
            if (repositorioLote.existsByEmpresaIdAndCodigo(empresaId, codigoNuevo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
            }
            return;
        }

        Long empresaActualId = loteActual.getEmpresa() != null ? loteActual.getEmpresa().getId() : null;
        boolean cambioEmpresa = empresaActualId == null || !empresaActualId.equals(empresaId);
        boolean cambioCodigo = !loteActual.getCodigo().equals(codigoNuevo);
        if ((cambioEmpresa || cambioCodigo) && repositorioLote.existsByEmpresaIdAndCodigo(empresaId, codigoNuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
        }
    }

    private EstadoAsignacionLote resolverEstadoAsignacion(String estadoAsignacion) {
        String valor = normalizarTexto(estadoAsignacion);
        if (valor == null) {
            return null;
        }
        String normalizadoEnum = valor.toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return EstadoAsignacionLote.valueOf(normalizadoEnum);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de asignacion invalido");
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

