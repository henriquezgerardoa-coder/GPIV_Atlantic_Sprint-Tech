package com.gpiv.atlanticsprinttech.backend.proyecto.persistence;

import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.proyecto.ProyectoProductivo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioProyecto extends JpaRepository<ProyectoProductivo, Long> {

    @Query(value = """
        SELECT p FROM ProyectoProductivo p
        JOIN FETCH p.empresa e
        JOIN FETCH p.lote l
        WHERE (:empresaId IS NULL OR e.id = :empresaId)
          AND (:estado IS NULL OR p.estado = :estado)
          AND (:desde IS NULL OR p.fechaCreacion >= :desde)
          AND (:hasta IS NULL OR p.fechaCreacion <= :hasta)
        ORDER BY p.fechaCreacion DESC
        """,
        countQuery = """
        SELECT COUNT(p) FROM ProyectoProductivo p
        WHERE (:empresaId IS NULL OR p.empresa.id = :empresaId)
          AND (:estado IS NULL OR p.estado = :estado)
          AND (:desde IS NULL OR p.fechaCreacion >= :desde)
          AND (:hasta IS NULL OR p.fechaCreacion <= :hasta)
        """)
    Page<ProyectoProductivo> buscarFiltrado(
        @Param("empresaId") Long empresaId,
        @Param("estado") EstadoProyecto estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM ProyectoProductivo p
        JOIN FETCH p.empresa
        JOIN FETCH p.lote
        WHERE p.id = :id
        """)
    Optional<ProyectoProductivo> findByIdConDependencias(@Param("id") Long id);

    @Query("""
        SELECT p FROM ProyectoProductivo p
        JOIN FETCH p.empresa e
        JOIN FETCH p.lote
        WHERE p.id = :id AND e.id = :empresaId
        """)
    Optional<ProyectoProductivo> findByIdAndEmpresaId(@Param("id") Long id, @Param("empresaId") Long empresaId);
}