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
        LEFT JOIN FETCH r.lote
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

    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa LEFT JOIN FETCH r.lote l LEFT JOIN FETCH l.empresa le LEFT JOIN FETCH le.rubro WHERE r.id = :id")
    Optional<RadicacionSolicitud> findByIdConEmpresa(@Param("id") Long id);

    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa LEFT JOIN FETCH r.lote l LEFT JOIN FETCH l.empresa le LEFT JOIN FETCH le.rubro WHERE r.id = :id AND r.empresa.id = :empresaId")
    Optional<RadicacionSolicitud> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    boolean existsByEmpresaIdAndEstado(Long empresaId, EstadoRadicacion estado);

    @Query("SELECT DISTINCT r.empresa.id FROM RadicacionSolicitud r WHERE r.empresa.id IN :ids AND r.estado = :estado")
    java.util.Set<Long> findEmpresaIdsConEstado(@Param("ids") List<Long> ids, @Param("estado") EstadoRadicacion estado);

    Optional<RadicacionSolicitud> findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(Long empresaId);

    boolean existsByEmpresaId(Long empresaId);

    /** Cuenta radicaciones agrupadas por estado — para informes/estadisticas (R-14). */
    @Query("SELECT r.estado, COUNT(r) FROM RadicacionSolicitud r GROUP BY r.estado")
    List<Object[]> contarPorEstado();

    @Query("SELECT r.numeroRadicado, r.fechaPlazo FROM RadicacionSolicitud r WHERE r.fechaPlazo IS NOT NULL")
    List<Object[]> findNumeroRadicadoYFechaPlazo();

    @Query("SELECT COALESCE(SUM(r.empleadosPrevistos), 0) FROM RadicacionSolicitud r WHERE r.estado IN :estados AND r.empleadosPrevistos IS NOT NULL")
    Long sumEmpleadosPrevistos(@Param("estados") List<EstadoRadicacion> estados);

    @Query("SELECT COUNT(r) FROM RadicacionSolicitud r WHERE r.estado NOT IN :estados")
    long countActivas(@Param("estados") List<EstadoRadicacion> estados);

    @Query("""
        SELECT r FROM RadicacionSolicitud r
        JOIN FETCH r.empresa
        WHERE r.lote.id = :loteId AND r.id <> :excludeId
        ORDER BY r.fechaRadicacion ASC
        """)
    List<RadicacionSolicitud> findByLoteIdExcluyendo(@Param("loteId") Long loteId, @Param("excludeId") Long excludeId);

    @Query("""
        SELECT r FROM RadicacionSolicitud r
        JOIN FETCH r.empresa
        WHERE r.lote.id = :loteId
        ORDER BY r.fechaRadicacion ASC
        """)
    List<RadicacionSolicitud> findByLoteIdConEmpresa(@Param("loteId") Long loteId);
}
