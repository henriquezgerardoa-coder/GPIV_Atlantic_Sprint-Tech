package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepositorioRadicacionSolicitud extends JpaRepository<RadicacionSolicitud, Long> {

    boolean existsByEmpresaIdAndEstado(Long empresaId, EstadoRadicacion estado);
    Optional<RadicacionSolicitud> findFirstByEmpresaIdOrderByFechaUltimaActualizacionDesc(Long empresaId);

    // Verificación básica de existencia
    boolean existsByNumeroRadicado(String numeroRadicado);

    // Traer la solicitud cruzando los datos con la tabla de empresas
    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE r.id = :id")
    Optional<RadicacionSolicitud> findByIdConEmpresa(@Param("id") Long id);

    // Traer la solicitud validando al mismo tiempo que pertenezca a la empresa indicada
    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE r.id = :id AND r.empresa.id = :empresaId")
    Optional<RadicacionSolicitud> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    // Buscador dinámico con múltiples filtros opcionales
    @Query("SELECT r FROM RadicacionSolicitud r JOIN FETCH r.empresa WHERE " +
            "(:empresaId IS NULL OR r.empresa.id = :empresaId) AND " +
            "(:estado IS NULL OR r.estado = :estado) AND " +
            "(cast(:fechaInicio as date) IS NULL OR cast(r.fechaUltimaActualizacion as date) >= :fechaInicio) AND " +
            "(cast(:fechaFin as date) IS NULL OR cast(r.fechaUltimaActualizacion as date) <= :fechaFin)")
    List<RadicacionSolicitud> buscarFiltrado(
            @Param("empresaId") Long empresaId,
            @Param("estado") EstadoRadicacion estado,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    // Conteo de solicitudes agrupadas por estado (Para estadísticas/Dashboards)
    @Query("SELECT r.estado AS estado, COUNT(r) AS cantidad FROM RadicacionSolicitud r GROUP BY r.estado")
    List<Object[]> contarPorEstado();

}