package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPersonalRegistrado extends JpaRepository<PersonalRegistrado, Long> {

    List<PersonalRegistrado> findByEmpresaIdOrderByNombreCompletoAsc(Long empresaId);

    void deleteByEmpresaId(Long empresaId);

    Optional<PersonalRegistrado> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndCuit(Long empresaId, String cuit);

    java.util.Optional<com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado> findByEmpresaIdAndCuit(Long empresaId, String cuit);

    long countByEmpresaIdAndDeclarado(Long empresaId, boolean declarado);

    List<PersonalRegistrado> findByEmpresaIdAndDeclaradoFalseOrderByNombreCompletoAsc(Long empresaId);

    Optional<PersonalRegistrado> findTopByEmpresaIdAndDeclaradoTrueOrderByFechaDeclaracionDesc(Long empresaId);
}
