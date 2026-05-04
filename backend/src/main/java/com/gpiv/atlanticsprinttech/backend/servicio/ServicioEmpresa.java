package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ServicioEmpresa {
	List<Empresa> listar();
	Empresa obtenerPorId(Long id);
	Empresa crear(Empresa empresa);
	Empresa actualizar(Long id, Empresa empresa);
	void eliminar(Long id);
	void solicitarAmpliacionOCambioRubro(Long idEmpresa, Long idNuevoRubro, String justificacion);
}