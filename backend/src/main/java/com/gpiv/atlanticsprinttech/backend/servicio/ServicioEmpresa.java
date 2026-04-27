package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;

public interface ServicioEmpresa {
	List<Empresa> listar(String identificadorIngreso);
	Empresa obtenerPorId(Long id, String identificadorIngreso);
	Empresa crear(Empresa empresa, String identificadorIngreso);
	Empresa actualizar(Long id, Empresa empresa, String identificadorIngreso);
	void eliminar(Long id, String identificadorIngreso);
}