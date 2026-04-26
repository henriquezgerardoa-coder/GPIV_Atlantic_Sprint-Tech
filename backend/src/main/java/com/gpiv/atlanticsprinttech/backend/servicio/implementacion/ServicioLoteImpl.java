package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
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

    public ServicioLoteImpl(RepositorioLote repositorioLote, RepositorioEmpresa repositorioEmpresa) {
        this.repositorioLote = repositorioLote;
        this.repositorioEmpresa = repositorioEmpresa;
    }

    @Override
    public List<Lote> listar() {
        return repositorioLote.findAllConEmpresa();
    }

    @Override
    public Lote obtenerPorId(Long id) {
        return repositorioLote.findByIdConEmpresa(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
    }

    @Override
    public Lote crear(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Long empresaId) {
        Empresa empresa = obtenerEmpresa(empresaId);
        validarCodigoDisponible(empresaId, codigo, null);
        Lote lote = Lote.crear(codigo, superficieMetrosCuadrados, ocupado, empresa);
        Lote guardado = repositorioLote.save(lote);
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    public Lote actualizar(Long id, String codigo, Double superficieMetrosCuadrados, boolean ocupado, Long empresaId) {
        Lote loteActual = obtenerPorId(id);
        Empresa empresa = obtenerEmpresa(empresaId);
        validarCodigoDisponible(empresaId, codigo, loteActual);
        loteActual.actualizarDatos(codigo, superficieMetrosCuadrados, ocupado, empresa);
        Lote guardado = repositorioLote.save(loteActual);
        return repositorioLote.findByIdConEmpresa(guardado.getId()).orElse(guardado);
    }

    @Override
    public void eliminar(Long id) {
        Lote loteActual = obtenerPorId(id);
        repositorioLote.delete(loteActual);
    }

    private Empresa obtenerEmpresa(Long empresaId) {
        return repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa indicada no existe"));
    }

    private void validarCodigoDisponible(Long empresaId, String codigoNuevo, Lote loteActual) {
        if (loteActual == null) {
            if (repositorioLote.existsByEmpresaIdAndCodigo(empresaId, codigoNuevo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
            }
            return;
        }

        boolean cambioEmpresa = !loteActual.getEmpresa().getId().equals(empresaId);
        boolean cambioCodigo = !loteActual.getCodigo().equals(codigoNuevo);
        if ((cambioEmpresa || cambioCodigo) && repositorioLote.existsByEmpresaIdAndCodigo(empresaId, codigoNuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote con ese codigo para la empresa");
        }
    }
}

