package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleado;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleadosCantidad;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaResumenDeclaracion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import java.util.List;

public interface ServicioEmpleado {

	Empleado crear(Long empresaId, SolicitudEmpleado solicitud, String identificadorIngreso);

	RespuestaEmpleado obtenerPorId(Long empresaId, Long empleadoId, String identificadorIngreso);

	List<RespuestaEmpleado> listarPorEmpresa(Long empresaId, String identificadorIngreso);

	RespuestaEmpleadosCantidad obtenerCantidadPorEmpresa(Long empresaId, String identificadorIngreso);

	Empleado actualizar(Long empresaId, Long empleadoId, SolicitudEmpleado solicitud, String identificadorIngreso);

	void eliminar(Long empresaId, Long empleadoId, String identificadorIngreso);

	RespuestaResumenDeclaracion obtenerResumenDeclaracion(Long empresaId, String identificadorIngreso);

	RespuestaCenso declararPendientes(Long empresaId, String identificadorIngreso);
}

