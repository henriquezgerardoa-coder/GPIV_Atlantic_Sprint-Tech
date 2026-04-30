package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;

public interface ServicioEmpresa {
	List<Empresa> listar();
	Empresa obtenerPorId(Long id);
	Empresa crear(Empresa empresa);
	Empresa actualizar(Long id, Empresa empresa);
	void eliminar(Long id);
}