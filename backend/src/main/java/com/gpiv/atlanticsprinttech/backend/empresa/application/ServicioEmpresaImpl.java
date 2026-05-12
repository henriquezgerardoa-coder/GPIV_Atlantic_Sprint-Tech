package com.gpiv.atlanticsprinttech.backend.empresa.application;

import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioRubro;
import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioSolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.backend.lote.persistence.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.radicacion.persistence.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.usuario.persistence.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.utilidades.ExtractorJson;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.*;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.RespuestaRadicacionResumen;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import com.gpiv.atlanticsprinttech.entities.empresa.Rubro;
import com.gpiv.atlanticsprinttech.entities.empresa.SolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.usuario.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import java.util.Comparator;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioEmpresaImpl implements ServicioEmpresa {
	private final RepositorioEmpresa repositorioEmpresa;
	private final RepositorioLote repositorioLote;
	private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
	private final RepositorioUsuario repositorioUsuario;
	private final ServicioContextoUsuario servicioContextoUsuario;
	private final ObjectMapper objectMapper;
	private final RepositorioRubro repositorioRubro;
	private final RepositorioSolicitudCambioRubro repositorioSolicitudCambioRubro;

	public ServicioEmpresaImpl(
		RepositorioEmpresa repositorioEmpresa,
		RepositorioLote repositorioLote,
		RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
		RepositorioUsuario repositorioUsuario,
		ServicioContextoUsuario servicioContextoUsuario,
		ObjectMapper objectMapper,
		RepositorioRubro repositorioRubro,
		RepositorioSolicitudCambioRubro repositorioSolicitudCambioRubro
	) {
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioLote = repositorioLote;
		this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
		this.repositorioUsuario = repositorioUsuario;
		this.servicioContextoUsuario = servicioContextoUsuario;
		this.objectMapper = objectMapper;
		this.repositorioRubro = repositorioRubro;
		this.repositorioSolicitudCambioRubro = repositorioSolicitudCambioRubro;
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
		validarNoDirectivoEnMutacion(usuario);
		if (servicioContextoUsuario.esRolEmpresa(usuario) && usuario.getEmpresaId() != null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario EMPRESA ya tiene una empresa asociada");
		}
		validarCuitDisponible(empresa.getCuit(), null);
		validarNitDisponible(empresa.getNit(), null);
		Empresa guardada = repositorioEmpresa.save(empresa);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			usuario.actualizarEmpresa(guardada);
			repositorioUsuario.save(usuario);
		}
		return guardada;
	}
	@Override
	public Empresa actualizar(Long id, Empresa empresa, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarNoDirectivoEnMutacion(usuario);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
			if (!empresaId.equals(id)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar otra empresa");
			}
			if (repositorioRadicacionSolicitud.existsByEmpresaIdAndEstado(id, EstadoRadicacion.RADICADA)) {
				throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Los datos generales de la empresa no son editables luego de la radicacion"
				);
			}
		}
		Empresa empresaActual = obtenerPorIdInterno(id);
		validarCuitDisponible(empresa.getCuit(), empresaActual.getCuit());
		validarNitDisponible(empresa.getNit(), empresaActual.getNit());
		empresaActual.actualizarDatos(
			empresa.getNombre(),
			empresa.getRazonSocial(),
			empresa.getNit(),
			empresa.getCuit(),
			empresa.getDireccion(),
			empresa.getActividadEconomica(),
			empresa.getCorreoElectronico(),
			empresa.getTelefono()
		);
		return repositorioEmpresa.save(empresaActual);
	}
	@Override
	public void eliminar(Long id, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarNoDirectivoEnMutacion(usuario);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede eliminar empresas");
		}
		Empresa empresaActual = obtenerPorIdInterno(id);
		if (repositorioLote.existsByEmpresaId(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar la empresa porque tiene lotes asociados");
		}
		repositorioEmpresa.delete(empresaActual);
	}

	@Override
	public List<RespuestaEmpresaListadoAdmin> listarVistaAdmin(String identificadorIngreso) {
		validarAccesoAdmin(identificadorIngreso);
		return repositorioEmpresa.findAll().stream()
			.sorted(Comparator.comparing(Empresa::getNombre, String.CASE_INSENSITIVE_ORDER))
			.map(empresa -> new RespuestaEmpresaListadoAdmin(empresa.getId(), empresa.getNombre()))
			.toList();
	}

	@Override
	public RespuestaEmpresaDetalleAdmin obtenerDetalleVistaAdmin(Long empresaId, String identificadorIngreso) {
		validarAccesoAdmin(identificadorIngreso);
		Empresa empresa = obtenerPorIdInterno(empresaId);
		List<RespuestaVehiculoEmpresa> vehiculos = leerVehiculos(empresa.getVehiculosAsignadosJson());
		List<RespuestaUsuarioEmpresaAdmin> usuariosAsociados = construirUsuariosAsociados(empresaId);
		String estadoExpediente = repositorioRadicacionSolicitud
			.findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(empresaId)
			.map(RadicacionSolicitud::getEstado).map(Enum::name).orElse(null);
		List<RespuestaRadicacionResumen> radicaciones = construirRadicaciones(empresaId);

		return new RespuestaEmpresaDetalleAdmin(
			empresa.getId(),
			empresa.getNombre(),
			empresa.getRazonSocial(),
			empresa.getNit(),
			empresa.getCuit(),
			empresa.getTelefono(),
			empresa.getDireccion(),
			empresa.getFechaRegistro(),
			empresa.getEstado(),
			empresa.getActividadEconomica(),
			empresa.getCorreoElectronico(),
			empresa.getCantidadEmpleadosOCero(),
			vehiculos.size(),
			estadoExpediente,
			usuariosAsociados,
			vehiculos,
			radicaciones
		);
	}

	@Override
	public boolean permiteServiciosPostRadicacion(Long empresaId, String identificadorIngreso) {
		validarAccesoEmpresa(empresaId, identificadorIngreso);
		return repositorioRadicacionSolicitud.existsByEmpresaIdAndEstado(empresaId, EstadoRadicacion.RADICADA);
	}

	@Override
	public RespuestaServiciosPostRadicacion obtenerServiciosPostRadicacion(Long empresaId, String identificadorIngreso) {
		validarAccesoEmpresa(empresaId, identificadorIngreso);
		Empresa empresa = obtenerPorIdInterno(empresaId);
		if (!repositorioRadicacionSolicitud.existsByEmpresaIdAndEstado(empresaId, EstadoRadicacion.RADICADA)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La empresa aun no tiene un expediente en estado RADICADA");
		}
		return new RespuestaServiciosPostRadicacion(
			empresa.getId(),
			empresa.getCantidadEmpleadosOCero(),
			leerVehiculos(empresa.getVehiculosAsignadosJson())
		);
	}

	@Override
	public RespuestaServiciosPostRadicacion actualizarServiciosPostRadicacion(
		Long empresaId,
		SolicitudServiciosPostRadicacion solicitud,
		String identificadorIngreso
	) {
		validarNoDirectivoEnMutacion(servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso));
		validarAccesoEmpresa(empresaId, identificadorIngreso);
		if (!repositorioRadicacionSolicitud.existsByEmpresaIdAndEstado(empresaId, EstadoRadicacion.RADICADA)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes gestionar servicios despues de la radicacion");
		}

		List<RespuestaVehiculoEmpresa> vehiculos = (solicitud.vehiculos() == null ? List.of() : solicitud.vehiculos()
			.stream()
			.map(v -> new RespuestaVehiculoEmpresa(v.placa().trim().toUpperCase(), v.tipo().trim(), v.descripcion() == null ? null : v.descripcion().trim()))
			.toList());
		validarVehiculos(vehiculos);

		Empresa empresa = obtenerPorIdInterno(empresaId);
		empresa.actualizarServiciosPostRadicacion(solicitud.cantidadEmpleados(), serializarVehiculos(vehiculos));
		Empresa guardada = repositorioEmpresa.save(empresa);
		return new RespuestaServiciosPostRadicacion(
			guardada.getId(),
			guardada.getCantidadEmpleadosOCero(),
			leerVehiculos(guardada.getVehiculosAsignadosJson())
		);
	}

	@Override
	public List<Empresa> buscar(String query) {
		if (query == null || query.isBlank()) return List.of();
		return repositorioEmpresa.buscarPorTermino(query.trim());
	}

	@Override
	public void vincularEmpresaExistente(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo usuarios con rol EMPRESA pueden vincularse a una empresa");
		}
		if (usuario.getEmpresaId() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya tiene una empresa asociada");
		}
		Empresa empresa = obtenerPorIdInterno(empresaId);
		usuario.actualizarEmpresa(empresa);
		repositorioUsuario.save(usuario);
	}

	@Override
	@Transactional
	public void solicitarAmpliacionOCambioRubro(Long empresaId, SolicitudCambioRubroDto solicitud, String identificadorIngreso) {
		validarAccesoEmpresa(empresaId, identificadorIngreso);
		Empresa empresa = obtenerPorIdInterno(empresaId);

		Rubro nuevoRubro = repositorioRubro.findById(solicitud.idNuevoRubro())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El rubro solicitado no existe"));
		if (empresa.getRubro() != null && empresa.getRubro().getId().equals(nuevoRubro.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa ya pertenece a este rubro");
		}
		SolicitudCambioRubro nuevaSolicitud = SolicitudCambioRubro.crear(empresa, nuevoRubro, solicitud.justificacion());
		repositorioSolicitudCambioRubro.save(nuevaSolicitud);
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

	private void validarNitDisponible(String nitNuevo, String nitActual) {
		boolean cambioDeNit = nitActual == null || !nitActual.equals(nitNuevo);
		if (cambioDeNit && repositorioEmpresa.existsByNit(nitNuevo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese NIT");
		}
	}

	private void validarAccesoEmpresa(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			Long empresaUsuario = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
			if (!empresaUsuario.equals(empresaId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar otra empresa");
			}
		}
	}

	/**
	 * TDA: Delega en el propio Usuario la decisión sobre si es administrador.
	 */
	private void validarAccesoAdmin(String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (!usuario.esAdministrador()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMINISTRADOR puede acceder a esta vista");
		}
	}

	/**
	 * TDA: Delega en el propio Usuario la decisión sobre si es directivo.
	 */
	private void validarNoDirectivoEnMutacion(Usuario usuario) {
		if (usuario.esDirectivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol DIRECTIVO tiene permisos de solo lectura para empresas");
		}
	}

	private List<RespuestaUsuarioEmpresaAdmin> construirUsuariosAsociados(Long empresaId) {
		return repositorioUsuario.findByEmpresa_IdOrderByIdAsc(empresaId).stream()
			.map(usuario -> new RespuestaUsuarioEmpresaAdmin(
				usuario.getNombreUsuario(),
				usuario.getNombreCompleto(),
				usuario.getCorreoElectronico(),
				usuario.getRoles(),
				usuario.estaActivo(),
				usuario.getFechaUltimoAcceso()
			))
			.toList();
	}

	private List<RespuestaRadicacionResumen> construirRadicaciones(Long empresaId) {
		return repositorioRadicacionSolicitud
			.buscarFiltrado(empresaId, null, null, null, org.springframework.data.domain.Pageable.unpaged())
			.stream()
			.map(r -> new RespuestaRadicacionResumen(
				r.getId(),
				r.getNumeroRadicado(),
				r.getTipoSolicitud(),
				r.getEstado().name(),
				r.getFechaRadicacion(),
				r.getFechaUltimaActualizacion(),
				ExtractorJson.extraerNecesidadMetrosCuadrados(r.getRelevamientoPedidoLotesJson())
			))
			.toList();
	}

	private void validarVehiculos(List<RespuestaVehiculoEmpresa> vehiculos) {
		long distintas = vehiculos.stream().map(RespuestaVehiculoEmpresa::placa).distinct().count();
		if (distintas < vehiculos.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten placas duplicadas");
		}
	}

	private List<RespuestaVehiculoEmpresa> leerVehiculos(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<RespuestaVehiculoEmpresa>>() {});
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer la lista de vehiculos");
		}
	}

	private String serializarVehiculos(List<RespuestaVehiculoEmpresa> vehiculos) {
		try {
			return objectMapper.writeValueAsString(vehiculos);
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo serializar la lista de vehiculos");
		}
	}
}