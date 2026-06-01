package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EvaluacionRadicacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioEvaluacionRadicacion extends JpaRepository<EvaluacionRadicacion, Long> {

    @Query("SELECT e FROM EvaluacionRadicacion e WHERE e.radicacion.id = :radicacionId")
    Optional<EvaluacionRadicacion> findByRadicacionId(@Param("radicacionId") Long radicacionId);

    @Query("SELECT e FROM EvaluacionRadicacion e WHERE e.radicacion.id IN :radicacionIds")
    List<EvaluacionRadicacion> findByRadicacionIdIn(@Param("radicacionIds") List<Long> radicacionIds);
}