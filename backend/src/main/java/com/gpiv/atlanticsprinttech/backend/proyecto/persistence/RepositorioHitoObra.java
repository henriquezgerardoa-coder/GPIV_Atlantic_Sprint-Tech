package com.gpiv.atlanticsprinttech.backend.proyecto.persistence;

import com.gpiv.atlanticsprinttech.entities.proyecto.HitoObra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioHitoObra extends JpaRepository<HitoObra, Long> {

    List<HitoObra> findByProyectoIdOrderByFechaVencimientoReal(Long proyectoId);

    @Query("SELECT h FROM HitoObra h WHERE h.id = :id AND h.proyecto.id = :proyectoId")
    Optional<HitoObra> findByIdAndProyectoId(@Param("id") Long id, @Param("proyectoId") Long proyectoId);
}