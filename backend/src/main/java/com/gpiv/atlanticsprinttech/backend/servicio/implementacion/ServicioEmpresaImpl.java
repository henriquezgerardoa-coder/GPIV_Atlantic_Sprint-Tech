package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRubro;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioSolicitudRadicacion;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;

import com.gpiv.atlanticsprinttech.entities.dominio.Rubro;
import com.gpiv.atlanticsprinttech.entities.dominio.SolicitudRadicacion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioEmpresaImpl implements ServicioEmpresa {
	private final RepositorioEmpresa repositorioEmpresa;
	private final RepositorioLote repositorioLote;
	private final RepositorioRubro repositorioRubro;
	private final RepositorioSolicitudRadicacion repositorioSolicitud;

	public ServicioEmpresaImpl(RepositorioEmpresa repositorioEmpresa, RepositorioLote repositorioLote
								, RepositorioRubro repositorioRubro, RepositorioSolicitudRadicacion repositorioSolicitud) {
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioLote = repositorioLote;
		this.repositorioRubro = repositorioRubro;
		this.repositorioSolicitud = repositorioSolicitud;
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
		empresaActual.actualizarDatosContacto(empresa.getNombre(), empresa.getCuit());
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

	@Override
	public void solicitarAmpliacionOCambioRubro(Long idEmpresa, Long idNuevoRubro, String justificacion) {
		Empresa empresa = obtenerPorId(idEmpresa);
		Rubro nuevoRubro = repositorioRubro.findById(idNuevoRubro).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rubro no encontrado"));
		empresa.solicitarCambioRubro(nuevoRubro, justificacion);
		//codigo temporal para solicitud
		String numeroSolicitud = "AMP-" + System.currentTimeMillis();

		SolicitudRadicacion nuevaSolicitud = SolicitudRadicacion.crear(numeroSolicitud, empresa.getCuit(), empresa.getNombre()
																, justificacion, empresa.getCantidadEmpleados(), "Ampliación a Rubro: " + nuevoRubro.getNombre());
		//nuevaSolicitud.vincularConEmpresa(empresa);

		repositorioSolicitud.save(nuevaSolicitud)
	}


	private void validarCuitDisponible(String cuitNuevo, String cuitActual) {
		boolean cambioDeCuit = cuitActual == null || !cuitActual.equals(cuitNuevo);
		if (cambioDeCuit && repositorioEmpresa.existsByCuit(cuitNuevo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese CUIT");
		}
	}
}