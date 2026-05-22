package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoCambioRubro;
import com.gpiv.atlanticsprinttech.entities.dominio.SolicitudCambioRubro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioSolicitudCambioRubro extends JpaRepository<SolicitudCambioRubro, Long> {

    @Query("SELECT s FROM SolicitudCambioRubro s JOIN FETCH s.empresa JOIN FETCH s.rubroSolicitado ORDER BY s.fechaSolicitud DESC")
    List<SolicitudCambioRubro> findAllConDetalle();

    @Query("SELECT s FROM SolicitudCambioRubro s JOIN FETCH s.empresa JOIN FETCH s.rubroSolicitado WHERE s.empresa.id = :empresaId ORDER BY s.fechaSolicitud DESC")
    List<SolicitudCambioRubro> findByEmpresaIdConDetalle(@Param("empresaId") Long empresaId);

    boolean existsByEmpresaIdAndEstado(Long empresaId, EstadoCambioRubro estado);

    @Query("SELECT s FROM SolicitudCambioRubro s JOIN FETCH s.empresa JOIN FETCH s.rubroSolicitado WHERE s.id = :id")
    Optional<SolicitudCambioRubro> findByIdConDetalle(@Param("id") Long id);
}
