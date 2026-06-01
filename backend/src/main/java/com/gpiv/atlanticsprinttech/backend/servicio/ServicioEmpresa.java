package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaDetalleAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresaListadoAdmin;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;
import java.util.Set;

public interface ServicioEmpresa {
	List<Empresa> listar(String identificadorIngreso);
	Set<Long> empresasHabilitadasParaServiciosPostRadicacion(List<Long> empresaIds);
	List<Empresa> listarDisponibles();
	Empresa obtenerPorId(Long id, String identificadorIngreso);
	Empresa crear(Empresa empresa, String identificadorIngreso);
	Empresa actualizar(Long id, Empresa empresa, String identificadorIngreso);
	Empresa actualizarContacto(Long id, String correoElectronico, String telefono, String identificadorIngreso);
	void eliminar(Long id, String identificadorIngreso);
	List<RespuestaEmpresaListadoAdmin> listarVistaAdmin(String identificadorIngreso);
	RespuestaEmpresaDetalleAdmin obtenerDetalleVistaAdmin(Long empresaId, String identificadorIngreso);
	void asignarRubroInicial(Long empresaId, Long rubroId, String identificadorIngreso);
	boolean permiteServiciosPostRadicacion(Long empresaId, String identificadorIngreso);
	RespuestaServiciosPostRadicacion obtenerServiciosPostRadicacion(Long empresaId, String identificadorIngreso);
	RespuestaServiciosPostRadicacion actualizarServiciosPostRadicacion(Long empresaId, SolicitudServiciosPostRadicacion solicitud, String identificadorIngreso);
}