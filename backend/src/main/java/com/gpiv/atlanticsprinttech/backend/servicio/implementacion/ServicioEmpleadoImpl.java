package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpleado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpleado;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleado;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleadosCantidad;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioEmpleadoImpl implements ServicioEmpleado {

	private final RepositorioEmpleado repositorioEmpleado;
	private final RepositorioEmpresa repositorioEmpresa;
	private final ServicioContextoUsuario servicioContextoUsuario;
	private final ServicioAuditLog servicioAuditLog;

	public ServicioEmpleadoImpl(
		RepositorioEmpleado repositorioEmpleado,
		RepositorioEmpresa repositorioEmpresa,
		ServicioContextoUsuario servicioContextoUsuario,
		ServicioAuditLog servicioAuditLog
	) {
		this.repositorioEmpleado = repositorioEmpleado;
		this.repositorioEmpresa = repositorioEmpresa;
		this.servicioContextoUsuario = servicioContextoUsuario;
		this.servicioAuditLog = servicioAuditLog;
	}

	@Override
	public Empleado crear(Long empresaId, SolicitudEmpleado solicitud, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarEmpresaPropietaria(usuario, empresaId, "crear empleados");

		Empresa empresa = repositorioEmpresa.findById(empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

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
			UtilRed.obtenerIpActual()
		);

		return guardado;
	}

	@Override
	public RespuestaEmpleado obtenerPorId(Long empresaId, Long empleadoId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarEmpresaPropietaria(usuario, empresaId, "ver empleados");

		Empleado empleado = repositorioEmpleado.findByIdAndEmpresaId(empleadoId, empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

		return crearRespuesta(empleado);
	}

	@Override
	public List<RespuestaEmpleado> listarPorEmpresa(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarEmpresaPropietaria(usuario, empresaId, "listar empleados");

		List<Empleado> empleados = repositorioEmpleado.findByEmpresa_Id(empresaId);
		return empleados.stream()
			.map(this::crearRespuesta)
			.toList();
	}

	@Override
	public RespuestaEmpleadosCantidad obtenerCantidadPorEmpresa(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);

		if (!usuario.tieneRol(RolUsuario.ADMINISTRADOR) && !usuario.tieneRol(RolUsuario.DIRECTIVO)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMINISTRADOR o DIRECTIVO pueden acceder a este recurso");
		}

		Empresa empresa = repositorioEmpresa.findById(empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

		long cantidad = repositorioEmpleado.countByEmpresaId(empresaId);

		return new RespuestaEmpleadosCantidad(empresa.getId(), empresa.getNombre(), cantidad);
	}

	@Override
	public Empleado actualizar(Long empresaId, Long empleadoId, SolicitudEmpleado solicitud, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarEmpresaPropietaria(usuario, empresaId, "actualizar empleados");

		Empleado empleado = repositorioEmpleado.findByIdAndEmpresaId(empleadoId, empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

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
			UtilRed.obtenerIpActual()
		);

		return guardado;
	}

	@Override
	public void eliminar(Long empresaId, Long empleadoId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarEmpresaPropietaria(usuario, empresaId, "eliminar empleados");

		Empleado empleado = repositorioEmpleado.findByIdAndEmpresaId(empleadoId, empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ELIMINACION_EMPLEADO", "Empleado",
			empleado.getCuit(),
			"Empleado: " + empleado.getNombre() + " | CUIT=" + empleado.getCuit(),
			null,
			UtilRed.obtenerIpActual()
		);

		repositorioEmpleado.deleteById(empleadoId);
	}

	private void validarEmpresaPropietaria(Usuario usuario, Long empresaId, String accion) {
		if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el rol EMPRESA puede " + accion);
		}

		Long empresaIdUsuario = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
		if (!empresaIdUsuario.equals(empresaId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes " + accion + " de otra empresa");
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
}
