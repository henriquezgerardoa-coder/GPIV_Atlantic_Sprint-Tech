package com.gpiv.atlanticsprinttech.backend.lote.application;

import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.lote.persistence.RepositorioLote;
import com.gpiv.atlanticsprinttech.entities.lote.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.lote.ZonaParque;
import com.gpiv.atlanticsprinttech.backend.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.entities.compartido.ExcepcionNegocio;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<Lote> listar(String identificadorIngreso, Pageable pageable) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioLote.findAllByEmpresaIdConEmpresa(empresaId, pageable);
        }
        return repositorioLote.findAllConEmpresa(pageable);
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

        Lote lote = Lote.crear(codigo, superficieMetrosCuadrados, resolverZona(zona));

        try {
            EstadoAsignacionLote estado = resolverEstadoAsignacion(estadoAsignacion, empresaObjetivo);
            if (estado == EstadoAsignacionLote.PREADJUDICADO_A_SOLICITUD) {
                lote.preAdjudicarALaSolicitud(normalizarTexto(numeroExpedienteReferencia));
            } else if (estado == EstadoAsignacionLote.ASIGNADO_A_EMPRESA) {
                lote.asignarEmpresa(empresa, normalizarTexto(numeroExpedienteReferencia));
            } else if (estado == EstadoAsignacionLote.ADJUDICADO_RADICADA) {
                lote.adjudicarRadicada(empresa, normalizarTexto(numeroExpedienteReferencia));
            } else {
                lote.liberar();
            }
        } catch (ExcepcionNegocio e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        Lote guardado = repositorioLote.save(lote);
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    public Lote actualizar(
        Long id,
        String codigo,
        Double superficieMetrosCuadrados,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String zona,
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

        loteActual.actualizarDatos(codigo, superficieMetrosCuadrados, resolverZona(zona));

        try {
            EstadoAsignacionLote estado = resolverEstadoAsignacion(estadoAsignacion, empresaObjetivo);
            if (estado == EstadoAsignacionLote.PREADJUDICADO_A_SOLICITUD) {
                loteActual.preAdjudicarALaSolicitud(normalizarTexto(numeroExpedienteReferencia));
            } else if (estado == EstadoAsignacionLote.ASIGNADO_A_EMPRESA) {
                loteActual.asignarEmpresa(empresa, normalizarTexto(numeroExpedienteReferencia));
            } else if (estado == EstadoAsignacionLote.ADJUDICADO_RADICADA) {
                loteActual.adjudicarRadicada(empresa, normalizarTexto(numeroExpedienteReferencia));
            } else {
                loteActual.liberar();
            }
        } catch (ExcepcionNegocio e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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

            Long empresaActualId = loteActual.getEmpresaId();
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

        // TDA: el lote sabe su empresaId
        Long empresaActualId = loteActual.getEmpresaId();
        boolean cambioEmpresa = empresaActualId == null || !empresaActualId.equals(empresaId);
        boolean cambioCodigo = !loteActual.getCodigo().equals(codigoNuevo);
        if ((cambioEmpresa || cambioCodigo) && repositorioLote.existsByEmpresaIdAndCodigo(empresaId, codigoNuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
        }
    }

    private EstadoAsignacionLote resolverEstadoAsignacion(String estadoAsignacion, Long empresaId) {
        String valor = normalizarTexto(estadoAsignacion);
        if (valor == null) {
            // Si se provee empresa sin estado explícito, inferir ASIGNADO_A_EMPRESA
            return empresaId != null ? EstadoAsignacionLote.ASIGNADO_A_EMPRESA : EstadoAsignacionLote.DISPONIBLE;
        }
        String normalizadoEnum = valor.toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return EstadoAsignacionLote.valueOf(normalizadoEnum);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de asignacion invalido");
        }
    }

    private ZonaParque resolverZona(String zona) {
        String valor = normalizarTexto(zona);
        if (valor == null) return null;
        try {
            return ZonaParque.valueOf(valor.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona de parque invalida: " + valor);
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
