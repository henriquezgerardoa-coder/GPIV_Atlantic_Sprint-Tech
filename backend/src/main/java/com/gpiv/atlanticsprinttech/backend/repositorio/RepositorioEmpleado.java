package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioEmpleado extends JpaRepository<Empleado, Long> {

	List<Empleado> findByEmpresa_Id(Long empresaId);

	@Query("SELECT COUNT(e) FROM Empleado e WHERE e.empresa.id = :empresaId")
	long countByEmpresaId(@Param("empresaId") Long empresaId);

	Optional<Empleado> findByEmpresa_IdAndCuit(Long empresaId, String cuit);

	@Query("SELECT e FROM Empleado e WHERE e.empresa.id = :empresaId AND e.id = :empleadoId")
	Optional<Empleado> findByIdAndEmpresaId(@Param("empleadoId") Long empleadoId, @Param("empresaId") Long empresaId);

	boolean existsByEmpresa_IdAndCuit(Long empresaId, String cuit);
}

