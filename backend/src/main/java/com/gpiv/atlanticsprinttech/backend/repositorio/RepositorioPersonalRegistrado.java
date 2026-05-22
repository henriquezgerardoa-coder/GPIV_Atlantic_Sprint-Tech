package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPersonalRegistrado extends JpaRepository<PersonalRegistrado, Long> {

    List<PersonalRegistrado> findByEmpresaIdOrderByNombreCompletoAsc(Long empresaId);

    Optional<PersonalRegistrado> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndCuit(Long empresaId, String cuit);
}
