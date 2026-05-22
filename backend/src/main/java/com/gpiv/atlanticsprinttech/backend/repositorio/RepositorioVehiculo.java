package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Vehiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioVehiculo extends JpaRepository<Vehiculo, Long> {

    List<Vehiculo> findByEmpresaIdOrderByPatenteAsc(Long empresaId);

    Optional<Vehiculo> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndPatente(Long empresaId, String patente);
}
