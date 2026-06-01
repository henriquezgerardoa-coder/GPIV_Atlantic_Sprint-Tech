package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.cache.annotation.Cacheable;

public interface RepositorioEmpresa extends JpaRepository<Empresa, Long> {

	@Cacheable(value = "empresas", key = "#cuit")
	boolean existsByCuit(String cuit);

	@Cacheable(value = "empresas", key = "#correoElectronico.toLowerCase()")
	Optional<Empresa> findByCorreoElectronicoIgnoreCase(String correoElectronico);

	@Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.rubro ORDER BY e.nombre ASC")
	@Cacheable(value = "empresas", key = "'all'")
	List<Empresa> findAllWithRubroOrderByNombre();

	@Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.rubro WHERE e.id = :id")
	@Cacheable(value = "empresas", key = "#id")
	Optional<Empresa> findByIdWithRubro(@Param("id") Long id);

	@Query("SELECT e.id, e.nombre FROM Empresa e ORDER BY e.nombre ASC")
	@Cacheable(value = "empresas", key = "'idNombre'")
	List<Object[]> findAllIdNombre();
}