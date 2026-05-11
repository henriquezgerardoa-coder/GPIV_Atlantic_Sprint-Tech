package com.gpiv.atlanticsprinttech.backend.radicacion.persistence;

import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioRadicacionSolicitud extends JpaRepository<RadicacionSolicitud, Long> {

    boolean existsByNumeroRadicado(String numeroRadicado);

    @Query(value = """
        SELECT r FROM RadicacionSolicitud r
        JOIN FETCH r.empresa e
        WHERE (:empresaId IS NULL OR e.id = :empresaId)
          AND (:estado IS NULL OR r.estado = :estado)
          AND (:desde IS NULL OR r.fechaRadicacion >= :desde)
          AND (:hasta IS NULL OR r.fechaRadicacion <= :hasta)
        ORDER BY r.fechaUltimaActualizacion DESC
        """,
        countQuery = """
        SELECT COUNT(r) FROM RadicacionSolicitud r
        WHERE (:empresaId IS NULL OR r.empresa.id = :empresaId)
          AND (:estado IS NULL OR r.estado = :estado)
          AND (:desde IS NULL OR r.fechaRadicacion >= :desde)
          AND (:hasta IS NULL OR r.fechaRadicacion <= :hasta)
        """)
    Page<RadicacionSolicitud> buscarFiltrado(
        @Param("empresaId") Long empresaId,
        @Param("estado") EstadoRadicacion estado,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta,
        Pageable pageable
    );

    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE r.id = :id")
    Optional<RadicacionSolicitud> findByIdConEmpresa(@Param("id") Long id);

    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE r.id = :id AND r.empresa.id = :empresaId")
    Optional<RadicacionSolicitud> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RadicacionSolicitud r WHERE r.empresa.id = :empresaId AND r.estado = :estado")
    boolean existsByEmpresaIdAndEstado(@Param("empresaId") Long empresaId, @Param("estado") EstadoRadicacion estado);

    @Query("SELECT r FROM RadicacionSolicitud r WHERE r.empresa.id = :empresaId ORDER BY r.fechaUltimaActualizacion DESC LIMIT 1")
    Optional<RadicacionSolicitud> findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(@Param("empresaId") Long empresaId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RadicacionSolicitud r WHERE r.empresa.id = :empresaId")
    boolean existsByEmpresaId(@Param("empresaId") Long empresaId);

    /** Cuenta radicaciones agrupadas por estado — para informes/estadisticas (R-14). */
    @Query("SELECT r.estado, COUNT(r) FROM RadicacionSolicitud r GROUP BY r.estado")
    List<Object[]> contarPorEstado();
}
