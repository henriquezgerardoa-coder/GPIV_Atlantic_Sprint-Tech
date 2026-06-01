package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioCenso;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioPersonalRegistrado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpleado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpleado;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleado;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleadosCantidad;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaResumenDeclaracion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Censo;
import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioEmpleadoImpl implements ServicioEmpleado {

	private final RepositorioEmpleado repositorioEmpleado;
	private final RepositorioEmpresa repositorioEmpresa;
	private final RepositorioCenso repositorioCenso;
	private final ServicioContextoUsuario servicioContextoUsuario;
	private final ServicioAuditLog servicioAuditLog;
	private final RepositorioPersonalRegistrado repositorioPersonal;

	public ServicioEmpleadoImpl(
		RepositorioEmpleado repositorioEmpleado,
		RepositorioEmpresa repositorioEmpresa,
		RepositorioCenso repositorioCenso,
		RepositorioPersonalRegistrado repositorioPersonal,
		ServicioContextoUsuario servicioContextoUsuario,
		ServicioAuditLog servicioAuditLog
	) {
		this.repositorioEmpleado = repositorioEmpleado;
		this.repositorioEmpresa = repositorioEmpresa;
		this.repositorioCenso = repositorioCenso;
		this.repositorioPersonal = repositorioPersonal;
		this.servicioContextoUsuario = servicioContextoUsuario;
		this.servicioAuditLog = servicioAuditLog;
	}

	@Override
	public Empleado crear(Long empresaId, SolicitudEmpleado solicitud, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		validarEmpresaPropietaria(usuario, empresaId, "crear empleados");

		Empresa empresa = repositorioEmpresa.findById(empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

		String cuitFormateado = formatearCuit(solicitud.cuit());
		if (repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, cuitFormateado)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El CUIT ya está registrado para esta empresa");
		}

		Empleado empleado = Empleado.crear(cuitFormateado, solicitud.nombre(), empresa);
		Empleado guardado = repositorioEmpleado.save(empleado);

		servicioAuditLog.registrarEvento(
			identificadorIngreso, "CREACION_EMPLEADO", "Empleado",
			cuitFormateado,
			null,
			"Empleado: " + solicitud.nombre() + " | CUIT=" + cuitFormateado,
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

		String cuitFormateado = formatearCuit(solicitud.cuit());
		if (!empleado.getCuit().equals(cuitFormateado) &&
			repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, cuitFormateado)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El CUIT ya está registrado para esta empresa");
		}

		String anteriorDatos = "Empleado: " + empleado.getNombre() + " | CUIT=" + empleado.getCuit();
		empleado.actualizarDatos(cuitFormateado, solicitud.nombre());
		Empleado guardado = repositorioEmpleado.save(empleado);

		servicioAuditLog.registrarEvento(
			identificadorIngreso, "ACTUALIZACION_EMPLEADO", "Empleado",
			cuitFormateado,
			anteriorDatos,
			"Empleado: " + solicitud.nombre() + " | CUIT=" + cuitFormateado,
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

	@Override
	public RespuestaResumenDeclaracion obtenerResumenDeclaracion(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		servicioContextoUsuario.exigirAccesoAEmpresa(usuario, empresaId);

		long declarados = repositorioEmpleado.countByEmpresa_IdAndDeclarado(empresaId, true);
		long pendientes = repositorioEmpleado.countByEmpresa_IdAndDeclarado(empresaId, false);
		String ultimaDeclaracion = repositorioEmpleado
			.findTopByEmpresa_IdAndDeclaradoTrueOrderByFechaDeclaracionDesc(empresaId)
			.map(e -> e.getFechaDeclaracion().toString())
			.orElse(null);
		return new RespuestaResumenDeclaracion(declarados, pendientes, ultimaDeclaracion);
	}

	@Override
	public RespuestaCenso declararPendientes(Long empresaId, String identificadorIngreso) {
		Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
		if (!usuario.tieneRol(RolUsuario.EMPRESA)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"Solo el rol EMPRESA puede generar declaraciones juradas");
		}
		servicioContextoUsuario.exigirAccesoAEmpresa(usuario, empresaId);

		List<Empleado> pendientes = repositorioEmpleado
			.findByEmpresa_IdAndDeclaradoFalseOrderByNombreAsc(empresaId);
		if (pendientes.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				"No hay empleados pendientes de declarar");
		}

		pendientes.forEach(Empleado::declarar);
		repositorioEmpleado.saveAll(pendientes);

		// Sincronizar declaraciones con PersonalRegistrado (si existe registro de censo para ese CUIT)
		try {
			for (Empleado e : pendientes) {
				repositorioPersonal.findByEmpresaIdAndCuit(empresaId, e.getCuit())
					.ifPresentOrElse(pr -> {
						if (!pr.isDeclarado()) {
							pr.declarar();
							repositorioPersonal.save(pr);
						}
					}, () -> {
						// Si no existe PersonalRegistrado, crear uno y marcarlo como declarado
						com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado nuevo =
							com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado.crear(
								repositorioEmpresa.findById(empresaId).orElse(null),
								e.getCuit(), e.getNombre(), e.getFechaRegistro().toLocalDate()
							);
							// marcar como declarado antes de guardar
							nuevo.declarar();
							repositorioPersonal.save(nuevo);
					});
			}
			// Asegurar que todos los empleados declarados tengan su PersonalRegistrado marcado
			List<Empleado> declarados = repositorioEmpleado.findByEmpresa_IdAndDeclaradoTrue(empresaId);
			for (Empleado ed : declarados) {
				repositorioPersonal.findByEmpresaIdAndCuit(empresaId, ed.getCuit())
					.ifPresentOrElse(pr -> {
						if (!pr.isDeclarado()) {
							pr.declarar();
							repositorioPersonal.save(pr);
						}
					}, () -> {
						com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado nuevo =
							com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado.crear(
								repositorioEmpresa.findById(empresaId).orElse(null),
								ed.getCuit(), ed.getNombre(), ed.getFechaRegistro().toLocalDate()
							);
							nuevo.declarar();
							repositorioPersonal.save(nuevo);
					});
			}
		} catch (Exception ex) {
			servicioAuditLog.registrarEvento(
				identificadorIngreso, "CENSO_SYNC_FALLIDO", "DeclaracionEmpleados",
				null,
				null,
				"Error al sincronizar declaraciones con PersonalRegistrado: " + ex.getMessage(),
				UtilRed.obtenerIpActual()
			);
		}

		LocalDate hoy = LocalDate.now();
		int anio = hoy.getYear();
		int semestre = hoy.getMonthValue() <= 6 ? 1 : 2;

		Empresa empresa = repositorioEmpresa.findById(empresaId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

		repositorioCenso.findByEmpresaIdAndAnioPeriodoAndSemestre(empresaId, anio, semestre)
			.ifPresent(repositorioCenso::delete);

		long totalDeclarados = repositorioEmpleado.countByEmpresa_IdAndDeclarado(empresaId, true);
		String obs = "Declaración jurada S" + semestre + " " + anio
			+ " — " + pendientes.size() + " empleados incorporados"
			+ " (total declarados: " + totalDeclarados + ")";
		Censo censo = Censo.crear(empresa, anio, semestre, (int) totalDeclarados, 0, obs);
		Censo guardado = repositorioCenso.save(censo);

		return new RespuestaCenso(
			guardado.getId(),
			guardado.getAnioPeriodo(),
			guardado.getSemestre(),
			guardado.getFechaDeclaracion().toString(),
			guardado.getCantidadPersonalRegistrado(),
			guardado.getCantidadPersonalNoRegistrado(),
			guardado.calcularTotalEmpleados(),
			guardado.getObservacion()
		);
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

	private static String formatearCuit(String cuit) {
		String d = cuit.replaceAll("[^0-9]", "");
		return d.substring(0, 2) + "-" + d.substring(2, 10) + "-" + d.substring(10);
	}

	private RespuestaEmpleado crearRespuesta(Empleado empleado) {
		return new RespuestaEmpleado(
			empleado.getId(),
			empleado.getCuit(),
			empleado.getNombre(),
			empleado.getFechaRegistro(),
			empleado.isDeclarado()
		);
	}
}
