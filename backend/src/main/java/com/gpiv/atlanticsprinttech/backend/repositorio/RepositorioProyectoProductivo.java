package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioProyectoProductivo extends JpaRepository<ProyectoProductivo, Long> {

    @Query("""
        SELECT DISTINCT p FROM ProyectoProductivo p
        LEFT JOIN FETCH p.hitos
        LEFT JOIN FETCH p.responsableSeguimiento
        LEFT JOIN FETCH p.solicitudOrigen s
        LEFT JOIN FETCH s.empresa
        ORDER BY p.fechaCreacion DESC
        """)
    List<ProyectoProductivo> findAllConDetalle();

    @Query("""
        SELECT DISTINCT p FROM ProyectoProductivo p
        LEFT JOIN FETCH p.hitos
        LEFT JOIN FETCH p.responsableSeguimiento
        LEFT JOIN FETCH p.solicitudOrigen s
        LEFT JOIN FETCH s.empresa
        WHERE s.empresa.id = :empresaId
        ORDER BY p.fechaCreacion DESC
        """)
    List<ProyectoProductivo> findByEmpresaIdConDetalle(@Param("empresaId") Long empresaId);

    @Query("""
        SELECT p FROM ProyectoProductivo p
        LEFT JOIN FETCH p.hitos
        LEFT JOIN FETCH p.responsableSeguimiento
        LEFT JOIN FETCH p.solicitudOrigen s
        LEFT JOIN FETCH s.empresa
        WHERE p.id = :id
        """)
    Optional<ProyectoProductivo> findByIdConDetalle(@Param("id") Long id);

    boolean existsBySolicitudOrigenId(Long solicitudId);

    long countByEstadoNotIn(List<EstadoProyecto> estados);

    @Query("""
        SELECT COUNT(DISTINCT p) FROM ProyectoProductivo p
        JOIN p.hitos h
        WHERE h.cumplido = false
          AND h.fechaVencimientoReal < CURRENT_DATE
          AND p.estado NOT IN :estados
        """)
    long countConHitosVencidos(@Param("estados") List<EstadoProyecto> estados);
}
