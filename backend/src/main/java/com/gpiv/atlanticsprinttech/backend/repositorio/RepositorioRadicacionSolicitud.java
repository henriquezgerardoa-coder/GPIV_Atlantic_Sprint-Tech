package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioRadicacionSolicitud extends JpaRepository<RadicacionSolicitud, Long> {

    boolean existsByNumeroRadicado(String numeroRadicado);

    @Query("""
        SELECT r FROM RadicacionSolicitud r
        JOIN FETCH r.empresa e
        WHERE (:empresaId IS NULL OR e.id = :empresaId)
          AND (:estado IS NULL OR r.estado = :estado)
          AND (:desde IS NULL OR r.fechaRadicacion >= :desde)
          AND (:hasta IS NULL OR r.fechaRadicacion <= :hasta)
        ORDER BY r.fechaUltimaActualizacion DESC
        """)
    List<RadicacionSolicitud> buscarFiltrado(
        @Param("empresaId") Long empresaId,
        @Param("estado") EstadoRadicacion estado,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta
    );

    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE r.id = :id")
    Optional<RadicacionSolicitud> findByIdConEmpresa(@Param("id") Long id);

    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE r.id = :id AND r.empresa.id = :empresaId")
    Optional<RadicacionSolicitud> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    boolean existsByEmpresaIdAndEstado(Long empresaId, EstadoRadicacion estado);

    Optional<RadicacionSolicitud> findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(Long empresaId);

    boolean existsByEmpresaId(Long empresaId);

    /** Cuenta radicaciones agrupadas por estado — para informes/estadisticas (R-14). */
    @Query("SELECT r.estado, COUNT(r) FROM RadicacionSolicitud r GROUP BY r.estado")
    List<Object[]> contarPorEstado();
}
