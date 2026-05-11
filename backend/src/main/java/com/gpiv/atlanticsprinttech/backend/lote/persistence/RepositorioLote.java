package com.gpiv.atlanticsprinttech.backend.lote.persistence;

import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.entities.lote.EstadoAsignacionLote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioLote extends JpaRepository<Lote, Long> {

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lote l WHERE l.empresa.id = :empresaId AND l.codigo = :codigo")
    boolean existsByEmpresaIdAndCodigo(@Param("empresaId") Long empresaId, @Param("codigo") String codigo);

    boolean existsByEmpresaIsNullAndCodigo(String codigo);

    @Query("SELECT l FROM Lote l WHERE l.empresa.id = :empresaId AND l.codigo = :codigo")
    Optional<Lote> findByEmpresaIdAndCodigo(@Param("empresaId") Long empresaId, @Param("codigo") String codigo);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lote l WHERE l.empresa.id = :empresaId")
    boolean existsByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.id = :id")
    Optional<Lote> findByIdConEmpresa(@Param("id") Long id);

    @Query(value = "SELECT l FROM Lote l LEFT JOIN FETCH l.empresa",
           countQuery = "SELECT COUNT(l) FROM Lote l")
    Page<Lote> findAllConEmpresa(Pageable pageable);

    @Query(value = "SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.empresa.id = :empresaId",
           countQuery = "SELECT COUNT(l) FROM Lote l WHERE l.empresa.id = :empresaId")
    Page<Lote> findAllByEmpresaIdConEmpresa(@Param("empresaId") Long empresaId, Pageable pageable);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.id = :id AND l.empresa.id = :empresaId")
    Optional<Lote> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    long countByEstadoAsignacionIsNot(EstadoAsignacionLote estado);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.numeroExpedienteReferencia = :numeroExpediente AND l.estadoAsignacion = :estado")
    Optional<Lote> findByNumeroExpedienteReferenciaAndEstadoAsignacion(
        @Param("numeroExpediente") String numeroExpediente,
        @Param("estado") EstadoAsignacionLote estado
    );

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.estadoAsignacion = :estado ORDER BY l.codigo ASC")
    List<Lote> findAllByEstadoAsignacionConEmpresa(@Param("estado") EstadoAsignacionLote estado);
}
