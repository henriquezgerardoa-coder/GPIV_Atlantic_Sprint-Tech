package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Censo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCenso extends JpaRepository<Censo, Long> {

    List<Censo> findByEmpresaIdOrderByAnioPeriodoDesc(Long empresaId);

    Optional<Censo> findByEmpresaIdAndAnioPeriodo(Long empresaId, int anioPeriodo);
}
