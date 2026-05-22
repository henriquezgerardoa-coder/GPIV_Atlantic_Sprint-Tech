package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioHitoObra extends JpaRepository<HitoObra, Long> {

    Optional<HitoObra> findByIdAndProyectoId(Long id, Long proyectoId);

    @Query("""
        SELECT h FROM HitoObra h
        JOIN FETCH h.proyecto p
        LEFT JOIN FETCH p.solicitudOrigen s
        LEFT JOIN FETCH s.empresa
        WHERE h.cumplido = false
          AND h.fechaVencimientoReal < CURRENT_DATE
        ORDER BY h.fechaVencimientoReal ASC
        """)
    List<HitoObra> findHitosVencidos();
}
