package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioRadicacionHistorial extends JpaRepository<RadicacionHistorial, Long> {

    List<RadicacionHistorial> findByRadicacionIdOrderByFechaEventoDesc(Long radicacionId);

    @Query("""
        SELECT COUNT(h) > 0
        FROM RadicacionHistorial h
        WHERE h.radicacion.empresa.id = :empresaId
          AND h.estado = :estado
        """)
    boolean existsByEmpresaIdAndEstado(@Param("empresaId") Long empresaId, @Param("estado") EstadoRadicacion estado);

    @Query("SELECT DISTINCT h.radicacion.empresa.id FROM RadicacionHistorial h WHERE h.radicacion.empresa.id IN :ids AND h.estado = :estado")
    java.util.Set<Long> findEmpresaIdsConEstado(@Param("ids") java.util.List<Long> ids, @Param("estado") EstadoRadicacion estado);
}
