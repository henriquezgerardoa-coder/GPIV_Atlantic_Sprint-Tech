package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioConversacionMensajeria extends JpaRepository<ConversacionMensajeria, Long> {

    @Query("""
        SELECT c FROM ConversacionMensajeria c
        LEFT JOIN FETCH c.empresa
        JOIN FETCH c.usuarioResponsable
        WHERE c.empresa.id = :empresaId
        ORDER BY c.fechaUltimaActualizacion DESC
        """)
    List<ConversacionMensajeria> findByEmpresaIdConDetalleOrderByFechaUltimaActualizacionDesc(@Param("empresaId") Long empresaId);

    @Query("""
        SELECT c FROM ConversacionMensajeria c
        LEFT JOIN FETCH c.empresa
        JOIN FETCH c.usuarioResponsable
        ORDER BY c.fechaUltimaActualizacion DESC
        """)
    List<ConversacionMensajeria> findAllConDetalleOrderByFechaUltimaActualizacionDesc();

    @Query("""
        SELECT c FROM ConversacionMensajeria c
        LEFT JOIN FETCH c.empresa
        JOIN FETCH c.usuarioResponsable
        WHERE c.id = :id
        """)
    Optional<ConversacionMensajeria> findByIdConDetalle(@Param("id") Long id);

    @Query("""
        SELECT c FROM ConversacionMensajeria c
        LEFT JOIN FETCH c.empresa
        JOIN FETCH c.usuarioResponsable
        WHERE c.id = :id AND c.empresa.id = :empresaId
        """)
    Optional<ConversacionMensajeria> findByIdAndEmpresaIdConDetalle(@Param("id") Long id, @Param("empresaId") Long empresaId);
}

