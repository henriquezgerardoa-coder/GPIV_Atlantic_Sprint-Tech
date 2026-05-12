package com.gpiv.atlanticsprinttech.backend.empresa.persistence;

import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioEmpresa extends JpaRepository<Empresa, Long> {
	boolean existsByCuit(String cuit);
	boolean existsByNit(String nit);
	Optional<Empresa> findByCorreoElectronicoIgnoreCase(String correoElectronico);

	@Query("SELECT e FROM Empresa e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.razonSocial) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.cuit) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY e.nombre ASC")
	List<Empresa> buscarPorTermino(@Param("q") String q);
}