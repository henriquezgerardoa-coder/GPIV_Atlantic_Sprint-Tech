package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioLote extends JpaRepository<Lote, Long> {

    boolean existsByEmpresaIdAndCodigo(Long empresaId, String codigo);

    boolean existsByEmpresaIsNullAndCodigo(String codigo);

    Optional<Lote> findByEmpresaIdAndCodigo(Long empresaId, String codigo);

    boolean existsByEmpresaId(Long empresaId);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.id = :id")
    Optional<Lote> findByIdConEmpresa(@Param("id") Long id);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa")
    List<Lote> findAllConEmpresa();

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.empresa.id = :empresaId")
    List<Lote> findAllByEmpresaIdConEmpresa(@Param("empresaId") Long empresaId);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.id = :id AND l.empresa.id = :empresaId")
    Optional<Lote> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    /** Cuenta lotes ocupados — para estadisticas (R-14). */
    long countByOcupado(boolean ocupado);

    /** Cuenta lotes por estado de asignación — para estadisticas (R-14). */
    long countByEstadoAsignacion(EstadoAsignacionLote estado);
}

