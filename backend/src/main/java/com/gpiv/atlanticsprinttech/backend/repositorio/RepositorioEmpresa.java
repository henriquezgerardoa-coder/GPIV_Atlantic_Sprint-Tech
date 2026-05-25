package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioEmpresa extends JpaRepository<Empresa, Long> {
	boolean existsByCuit(String cuit);
	Optional<Empresa> findByCorreoElectronicoIgnoreCase(String correoElectronico);

	@Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.rubro ORDER BY e.nombre ASC")
	List<Empresa> findAllWithRubroOrderByNombre();

	@Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.rubro WHERE e.id = :id")
	Optional<Empresa> findByIdWithRubro(Long id);
}