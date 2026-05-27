package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioMensajeMensajeria extends JpaRepository<MensajeMensajeria, Long> {

    @Query("""
        SELECT m FROM MensajeMensajeria m
        LEFT JOIN FETCH m.usuarioEmisor
        WHERE m.conversacion.id = :conversacionId
        ORDER BY m.fechaEnvio ASC
        """)
    List<MensajeMensajeria> findByConversacionIdConEmisorOrderByFechaEnvioAsc(@Param("conversacionId") Long conversacionId);

    @Query("""
        SELECT m FROM MensajeMensajeria m
        LEFT JOIN FETCH m.usuarioEmisor
        WHERE m.conversacion.id IN :ids
        ORDER BY m.fechaEnvio ASC
        """)
    List<MensajeMensajeria> findByConversacionIdsConEmisor(@Param("ids") List<Long> ids);

    Optional<MensajeMensajeria> findFirstByConversacionIdOrderByFechaEnvioDesc(Long conversacionId);

    long countByConversacionId(Long conversacionId);
}

