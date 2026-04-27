package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioEmpresaImpl implements ServicioEmpresa {
	private final RepositorioEmpresa repositorioEmpresa;
	private final RepositorioLote repositorioLote;
	private final ServicioContextoUsuario servicioContextoUsuario;

	public ServicioEmpresaImpl(
		RepositorioEmpresa repositorioEmpresa,
		RepositorioLote repositorioLote,
		ServicioContextoUsuario servicioContextoUsuario
	) {
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioLote = repositorioLote;
		this.servicioContextoUsuario = servicioContextoUsuario;
	}
	@Override
	public List<Empresa> listar(String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
			Empresa empresa = obtenerPorIdInterno(empresaId);
			return List.of(empresa);
		}
		return repositorioEmpresa.findAll();
	}
	@Override
	public Empresa obtenerPorId(Long id, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
			if (!empresaId.equals(id)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes acceder a otra empresa");
			}
		}
		return obtenerPorIdInterno(id);
	}
	@Override
	public Empresa crear(Empresa empresa, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede crear empresas");
		}
		validarCuitDisponible(empresa.getCuit(), null);
		return repositorioEmpresa.save(empresa);
	}
	@Override
	public Empresa actualizar(Long id, Empresa empresa, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
			if (!empresaId.equals(id)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar otra empresa");
			}
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede modificar empresas");
		}
		Empresa empresaActual = obtenerPorIdInterno(id);
		validarCuitDisponible(empresa.getCuit(), empresaActual.getCuit());
		empresaActual.actualizarDatos(empresa.getNombre(), empresa.getCuit(), empresa.getCorreoElectronico());
		return repositorioEmpresa.save(empresaActual);
	}
	@Override
	public void eliminar(Long id, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede eliminar empresas");
		}
		Empresa empresaActual = obtenerPorIdInterno(id);
		if (repositorioLote.existsByEmpresaId(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar la empresa porque tiene lotes asociados");
		}
		repositorioEmpresa.delete(empresaActual);
	}

	private Empresa obtenerPorIdInterno(Long id) {
		return repositorioEmpresa.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
	}
	private void validarCuitDisponible(String cuitNuevo, String cuitActual) {
		boolean cambioDeCuit = cuitActual == null || !cuitActual.equals(cuitNuevo);
		if (cambioDeCuit && repositorioEmpresa.existsByCuit(cuitNuevo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese CUIT");
		}
	}
}