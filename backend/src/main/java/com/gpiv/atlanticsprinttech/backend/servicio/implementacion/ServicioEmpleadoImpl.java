package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpleado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpleado;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleado;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleadosCantidad;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioEmpleadoImpl implements ServicioEmpleado {

	private final RepositorioEmpleado repositorioEmpleado;
	private final RepositorioEmpresa repositorioEmpresa;
	private final RepositorioUsuario repositorioUsuario;
	private final ServicioContextoUsuario servicioContextoUsuario;
	private final ServicioAuditLog servicioAuditLog;

	public ServicioEmpleadoImpl(
		RepositorioEmpleado repositorioEmpleado,
		RepositorioEmpresa repositorioEmpresa,
		RepositorioUsuario repositorioUsuario,
		ServicioContextoUsuario servicioContextoUsuario,
		ServicioAuditLog servicioAuditLog
	) {
		this.repositorioEmpleado = repositorioEmpleado;
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioUsuario = repositorioUsuario;
		this.servicioContextoUsuario = servicioContextoUsuario;
		this.servicioAuditLog = servicioAuditLog;
	}

	@Override
	public Empleado crear(Long empresaId, SolicitudEmpleado solicitud, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);

		// Solo EMPRESA puede crear empleados en su empresa
		if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los usuarios de EMPRESA pueden crear empleados");
		}

		Long empresaIdUsuario = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
		if (!empresaIdUsuario.equals(empresaId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes crear empleados en otra empresa");
		}

		Empresa empresa = repositorioEmpresa.findById(empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

		// Validar que el CUIT no exista para esta empresa
		if (repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, solicitud.cuit())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El CUIT ya está registrado para esta empresa");
		}

		Empleado empleado = Empleado.crear(solicitud.cuit(), solicitud.nombre(), empresa);
		Empleado guardado = repositorioEmpleado.save(empleado);

		servicioAuditLog.registrarEvento(
			identificadorIngreso, "CREACION_EMPLEADO", "Empleado",
			solicitud.cuit(),
			null,
			"Empleado: " + solicitud.nombre() + " | CUIT=" + solicitud.cuit(),
			obtenerIpActual()
		);

		return guardado;
	}

	@Override
	public RespuestaEmpleado obtenerPorId(Long empresaId, Long empleadoId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarAccesoAEmpresa(usuario, empresaId);

		Empleado empleado = repositorioEmpleado.findByIdAndEmpresaId(empleadoId, empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

		return crearRespuesta(empleado);
	}

	@Override
	public List<RespuestaEmpleado> listarPorEmpresa(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarAccesoAEmpresa(usuario, empresaId);

		List<Empleado> empleados = repositorioEmpleado.findByEmpresa_Id(empresaId);
		return empleados.stream()
			.map(this::crearRespuesta)
			.toList();
	}

	@Override
	public RespuestaEmpleadosCantidad obtenerCantidadPorEmpresa(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);

		// ADMIN y DIRECTIVO siempre pueden ver la cantidad
		if (!usuario.tieneRol(RolUsuario.ADMINISTRADOR) && !usuario.tieneRol(RolUsuario.DIRECTIVO)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMIN y DIRECTIVO pueden acceder a este recurso");
		}

		Empresa empresa = repositorioEmpresa.findById(empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

		long cantidad = repositorioEmpleado.countByEmpresaId(empresaId);

		return new RespuestaEmpleadosCantidad(empresa.getId(), empresa.getNombre(), cantidad);
	}

	@Override
	public Empleado actualizar(Long empresaId, Long empleadoId, SolicitudEmpleado solicitud, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);

		// Solo EMPRESA puede actualizar empleados
		if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los usuarios de EMPRESA pueden actualizar empleados");
		}

		Long empresaIdUsuario = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
		if (!empresaIdUsuario.equals(empresaId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes actualizar empleados de otra empresa");
		}

		Empleado empleado = repositorioEmpleado.findByIdAndEmpresaId(empleadoId, empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

		// Validar que el nuevo CUIT no exista para esta empresa (si cambió)
		if (!empleado.getCuit().equals(solicitud.cuit()) &&
			repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, solicitud.cuit())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El CUIT ya está registrado para esta empresa");
		}

		String anteriorDatos = "Empleado: " + empleado.getNombre() + " | CUIT=" + empleado.getCuit();
		empleado.actualizarDatos(solicitud.cuit(), solicitud.nombre());
		Empleado guardado = repositorioEmpleado.save(empleado);

		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ACTUALIZACION_EMPLEADO", "Empleado",
			solicitud.cuit(),
			anteriorDatos,
			"Empleado: " + solicitud.nombre() + " | CUIT=" + solicitud.cuit(),
			obtenerIpActual()
		);

		return guardado;
	}

	@Override
	public void eliminar(Long empresaId, Long empleadoId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);

		// Solo EMPRESA puede eliminar empleados
		if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los usuarios de EMPRESA pueden eliminar empleados");
		}

		Long empresaIdUsuario = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
		if (!empresaIdUsuario.equals(empresaId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes eliminar empleados de otra empresa");
		}

		Empleado empleado = repositorioEmpleado.findByIdAndEmpresaId(empleadoId, empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ELIMINACION_EMPLEADO", "Empleado",
			empleado.getCuit(),
			"Empleado: " + empleado.getNombre() + " | CUIT=" + empleado.getCuit(),
			null,
			obtenerIpActual()
		);

		repositorioEmpleado.deleteById(empleadoId);
	}

	private void validarAccesoAEmpresa(Usuario usuario, Long empresaId) {
		if (servicioContextoUsuario.esRolEmpresa(usuario)) {
			Long empresaIdUsuario = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
			if (!empresaIdUsuario.equals(empresaId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes acceder a empleados de otra empresa");
			}
		} else if (!usuario.tieneRol(RolUsuario.ADMINISTRADOR) && !usuario.tieneRol(RolUsuario.DIRECTIVO)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a los empleados");
		}
	}

	private RespuestaEmpleado crearRespuesta(Empleado empleado) {
		return new RespuestaEmpleado(
			empleado.getId(),
			empleado.getCuit(),
			empleado.getNombre(),
			empleado.getFechaRegistro()
		);
	}

	private String obtenerIpActual() {
		try {
			ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest request = attrs.getRequest();
				String ip = request.getHeader("X-Forwarded-For");
				if (ip == null || ip.isEmpty()) {
					ip = request.getRemoteAddr();
				}
				return ip;
			}
		} catch (Exception e) {
			// Si no se puede obtener la IP, continuamos
		}
		return "DESCONOCIDA";
	}
}

