package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEmpleado;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleado;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleadosCantidad;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas/{empresaId}/empleados")
public class ControladorEmpleado {

	private final ServicioEmpleado servicioEmpleado;

	public ControladorEmpleado(ServicioEmpleado servicioEmpleado) {
		this.servicioEmpleado = servicioEmpleado;
	}

	/**
	 * Crear un nuevo empleado en una empresa
	 * Solo EMPRESA puede crear empleados
	 */
	@PostMapping
	public ResponseEntity<RespuestaEmpleado> crear(
		@PathVariable Long empresaId,
		@Valid @RequestBody SolicitudEmpleado solicitud,
		Authentication autenticacion
	) {
		Empleado empleadoCreado = servicioEmpleado.crear(empresaId, solicitud, autenticacion.getName());
		URI location = URI.create("/api/empresas/" + empresaId + "/empleados/" + empleadoCreado.getId());
		return ResponseEntity.created(location).body(toRespuesta(empleadoCreado));
	}

	/**
	 * Obtener un empleado específico por ID
	 * EMPRESA ve el detalle completo de sus empleados
	 * ADMIN y DIRECTIVO solo pueden acceder si es a través del endpoint de cantidad
	 */
	@GetMapping("/{empleadoId}")
	public RespuestaEmpleado obtenerPorId(
		@PathVariable Long empresaId,
		@PathVariable Long empleadoId,
		Authentication autenticacion
	) {
		return servicioEmpleado.obtenerPorId(empresaId, empleadoId, autenticacion.getName());
	}

	/**
	 * Listar todos los empleados de una empresa
	 * Solo EMPRESA puede ver el listado completo de sus empleados
	 */
	@GetMapping
	public List<RespuestaEmpleado> listar(
		@PathVariable Long empresaId,
		Authentication autenticacion
	) {
		return servicioEmpleado.listarPorEmpresa(empresaId, autenticacion.getName());
	}

	/**
	 * Obtener solo la cantidad de empleados
	 * Solo ADMIN y DIRECTIVO pueden acceder a este endpoint
	 */
	@GetMapping("/cantidad")
	public RespuestaEmpleadosCantidad obtenerCantidad(
		@PathVariable Long empresaId,
		Authentication autenticacion
	) {
		return servicioEmpleado.obtenerCantidadPorEmpresa(empresaId, autenticacion.getName());
	}

	/**
	 * Actualizar un empleado
	 * Solo EMPRESA puede actualizar sus empleados
	 */
	@PutMapping("/{empleadoId}")
	public RespuestaEmpleado actualizar(
		@PathVariable Long empresaId,
		@PathVariable Long empleadoId,
		@Valid @RequestBody SolicitudEmpleado solicitud,
		Authentication autenticacion
	) {
		Empleado empleadoActualizado = servicioEmpleado.actualizar(empresaId, empleadoId, solicitud, autenticacion.getName());
		return toRespuesta(empleadoActualizado);
	}

	/**
	 * Eliminar un empleado
	 * Solo EMPRESA puede eliminar sus empleados
	 */
	@DeleteMapping("/{empleadoId}")
	public ResponseEntity<Void> eliminar(
		@PathVariable Long empresaId,
		@PathVariable Long empleadoId,
		Authentication autenticacion
	) {
		servicioEmpleado.eliminar(empresaId, empleadoId, autenticacion.getName());
		return ResponseEntity.noContent().build();
	}

	private RespuestaEmpleado toRespuesta(Empleado empleado) {
		return new RespuestaEmpleado(
			empleado.getId(),
			empleado.getCuit(),
			empleado.getNombre(),
			empleado.getFechaRegistro()
		);
	}
}

