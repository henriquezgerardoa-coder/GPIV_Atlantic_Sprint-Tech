package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioLote extends JpaRepository<Lote, Long> {

    boolean existsByEmpresaIdAndCodigo(Long empresaId, String codigo);

    Optional<Lote> findByEmpresaIdAndCodigo(Long empresaId, String codigo);

    boolean existsByEmpresaId(Long empresaId);

    @Query("SELECT l FROM Lote l JOIN FETCH l.empresa WHERE l.id = :id")
    Optional<Lote> findByIdConEmpresa(@Param("id") Long id);

    @Query("SELECT l FROM Lote l JOIN FETCH l.empresa")
    List<Lote> findAllConEmpresa();
}

