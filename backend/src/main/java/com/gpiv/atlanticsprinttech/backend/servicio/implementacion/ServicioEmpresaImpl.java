package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpresa;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaDetalleAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaListadoAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaUsuarioEmpresaAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaVehiculoEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudServiciosPostRadicacion;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	private final ServicioAuditLog servicioAuditLog;
	private final ObjectMapper objectMapper;

	public ServicioEmpresaImpl(
		RepositorioEmpresa repositorioEmpresa,
		RepositorioLote repositorioLote,
		RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
		RepositorioUsuario repositorioUsuario,
		ServicioContextoUsuario servicioContextoUsuario,
		ServicioAuditLog servicioAuditLog,
		ObjectMapper objectMapper
	) {
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioLote = repositorioLote;
		this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
		this.repositorioUsuario = repositorioUsuario;
		this.servicioContextoUsuario = servicioContextoUsuario;
		this.servicioAuditLog = servicioAuditLog;
		this.objectMapper = objectMapper;
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
		servicioAuditLog.registrarEvento(
			identificadorIngreso, "CREACION_EMPRESA", "Empresa",
			guardada.getCuit(),
			null,
			guardada.getNombre() + " | CUIT=" + guardada.getCuit(),
			obtenerIpActual()
		);
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
		String anteriorDatos = empresaActual.getNombre() + " | CUIT=" + empresaActual.getCuit();
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
		Empresa guardada = repositorioEmpresa.save(empresaActual);
		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ACTUALIZACION_EMPRESA", "Empresa",
			guardada.getCuit(),
			anteriorDatos,
			guardada.getNombre() + " | CUIT=" + guardada.getCuit(),
			obtenerIpActual()
		);
		return guardada;
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
		String datosEmpresa = empresaActual.getNombre() + " | CUIT=" + empresaActual.getCuit();
		String cuit = empresaActual.getCuit();
		repositorioEmpresa.delete(empresaActual);
		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ELIMINACION_EMPRESA", "Empresa",
			cuit,
			datosEmpresa,
			null,
			obtenerIpActual()
		);
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
		RespuestaUsuarioEmpresaAdmin usuarioAsociado = construirUsuarioAsociado(empresaId);
		String estadoExpediente = repositorioRadicacionSolicitud.findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(empresaId)
			.map(RadicacionSolicitud::getEstado)
			.map(Enum::name)
			.orElse(null);

		return new RespuestaEmpresaDetalleAdmin(
			empresa.getId(),
			empresa.getNombre(),
			empresa.getRazonSocial(),
			empresa.getNit(),
			empresa.getCuit(),
			empresa.getTelefono(),
			empresa.getDireccion(),
			empresa.getFechaRegistro(),
			empresa.getStatus(),
			empresa.getActividadEconomica(),
			empresa.getCorreoElectronico(),
			empresa.getCantidadEmpleados() == null ? 0 : empresa.getCantidadEmpleados(),
			vehiculos.size(),
			estadoExpediente,
			usuarioAsociado,
			vehiculos
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
			empresa.getCantidadEmpleados() == null ? 0 : empresa.getCantidadEmpleados(),
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

		List<RespuestaVehiculoEmpresa> vehiculos = (solicitud.vehiculos() == null ? List.<RespuestaVehiculoEmpresa>of() : solicitud.vehiculos()
			.stream()
			.map(v -> new RespuestaVehiculoEmpresa(v.placa().trim().toUpperCase(), v.tipo().trim(), v.descripcion() == null ? null : v.descripcion().trim()))
			.toList());
		validarVehiculos(vehiculos);

		Empresa empresa = obtenerPorIdInterno(empresaId);
		List<RespuestaVehiculoEmpresa> vehiculosAnteriores = leerVehiculos(empresa.getVehiculosAsignadosJson());
		String anteriorServicios = "empleados=" + empresa.getCantidadEmpleados() + " | vehiculos=" + vehiculosAnteriores.size();
		empresa.actualizarServiciosPostRadicacion(solicitud.cantidadEmpleados(), serializarVehiculos(vehiculos));
		Empresa guardada = repositorioEmpresa.save(empresa);
		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ACTUALIZACION_SERVICIOS_EMPRESA", "Empresa",
			guardada.getCuit(),
			anteriorServicios,
			"empleados=" + guardada.getCantidadEmpleados() + " | vehiculos=" + vehiculos.size(),
			obtenerIpActual()
		);
		return new RespuestaServiciosPostRadicacion(
			guardada.getId(),
			guardada.getCantidadEmpleados(),
			leerVehiculos(guardada.getVehiculosAsignadosJson())
		);
	}

	private String obtenerIpActual() {
		var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) return "desconocida";
		HttpServletRequest request = attrs.getRequest();
		String forwarded = request.getHeader("X-Forwarded-For");
		return (forwarded != null && !forwarded.isBlank())
			? forwarded.split(",")[0].trim()
			: request.getRemoteAddr();
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

	private void validarAccesoAdmin(String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (!usuario.getRoles().contains(RolUsuario.ADMINISTRADOR)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMINISTRADOR puede acceder a esta vista");
		}
	}

	private void validarNoDirectivoEnMutacion(Usuario usuario) {
		if (usuario.getRoles().contains(RolUsuario.DIRECTIVO)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol DIRECTIVO tiene permisos de solo lectura para empresas");
		}
	}

	private RespuestaUsuarioEmpresaAdmin construirUsuarioAsociado(Long empresaId) {
		return repositorioUsuario.findByEmpresa_IdOrderByIdAsc(empresaId).stream()
			.filter(usuario -> usuario.getRoles().contains(RolUsuario.EMPRESA))
			.findFirst()
			.map(usuario -> new RespuestaUsuarioEmpresaAdmin(
				usuario.getNombreCompleto(),
				usuario.getCorreoElectronico(),
				usuario.getRoles(),
				usuario.isActivo(),
				usuario.getFechaUltimoAcceso()
			))
			.orElse(null);
	}

	private void validarVehiculos(List<RespuestaVehiculoEmpresa> vehiculos) {
		Set<String> placas = new HashSet<>();
		for (RespuestaVehiculoEmpresa vehiculo : vehiculos) {
			if (!placas.add(vehiculo.placa())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten placas duplicadas");
			}
		}
	}

	private List<RespuestaVehiculoEmpresa> leerVehiculos(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<RespuestaVehiculoEmpresa>>() {
			});
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