package com.gpiv.atlanticsprinttech.backend.empresa.application;

import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaEmpresaDetalleAdmin;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.RespuestaEmpresaListadoAdmin;
import com.gpiv.atlanticsprinttech.commons.empresa.dto.SolicitudServiciosPostRadicacion;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import java.util.List;

public interface ServicioEmpresa {
	List<Empresa> listar(String identificadorIngreso);
	Empresa obtenerPorId(Long id, String identificadorIngreso);
	Empresa crear(Empresa empresa, String identificadorIngreso);
	Empresa actualizar(Long id, Empresa empresa, String identificadorIngreso);
	void eliminar(Long id, String identificadorIngreso);
	List<RespuestaEmpresaListadoAdmin> listarVistaAdmin(String identificadorIngreso);
	RespuestaEmpresaDetalleAdmin obtenerDetalleVistaAdmin(Long empresaId, String identificadorIngreso);
	boolean permiteServiciosPostRadicacion(Long empresaId, String identificadorIngreso);
	RespuestaServiciosPostRadicacion obtenerServiciosPostRadicacion(Long empresaId, String identificadorIngreso);
	RespuestaServiciosPostRadicacion actualizarServiciosPostRadicacion(Long empresaId, SolicitudServiciosPostRadicacion solicitud, String identificadorIngreso);
	List<Empresa> buscar(String query);
	void vincularEmpresaExistente(Long empresaId, String identificadorIngreso);
}