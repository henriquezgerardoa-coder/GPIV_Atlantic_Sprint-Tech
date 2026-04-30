package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioEmpresaImpl implements ServicioEmpresa {
	private final RepositorioEmpresa repositorioEmpresa;
	private final RepositorioLote repositorioLote;

	public ServicioEmpresaImpl(RepositorioEmpresa repositorioEmpresa, RepositorioLote repositorioLote) {
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioLote = repositorioLote;
	}
	@Override
	public List<Empresa> listar() {
		return repositorioEmpresa.findAll();
	}
	@Override
	public Empresa obtenerPorId(Long id) {
		return repositorioEmpresa.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
	}
	@Override
	public Empresa crear(Empresa empresa) {
		validarCuitDisponible(empresa.getCuit(), null);
		return repositorioEmpresa.save(empresa);
	}
	@Override
	public Empresa actualizar(Long id, Empresa empresa) {
		Empresa empresaActual = obtenerPorId(id);
		validarCuitDisponible(empresa.getCuit(), empresaActual.getCuit());
		empresaActual.actualizarDatos(empresa.getNombre(), empresa.getCuit(), empresa.getCorreoElectronico());
		return repositorioEmpresa.save(empresaActual);
	}
	@Override
	public void eliminar(Long id) {
		Empresa empresaActual = obtenerPorId(id);
		if (repositorioLote.existsByEmpresaId(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar la empresa porque tiene lotes asociados");
		}
		repositorioEmpresa.delete(empresaActual);
	}
	private void validarCuitDisponible(String cuitNuevo, String cuitActual) {
		boolean cambioDeCuit = cuitActual == null || !cuitActual.equals(cuitNuevo);
		if (cambioDeCuit && repositorioEmpresa.existsByCuit(cuitNuevo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese CUIT");
		}
	}
}