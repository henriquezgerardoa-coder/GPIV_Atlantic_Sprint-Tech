package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Servicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioServicio extends JpaRepository<Servicio, Long> {

    @Query("SELECT s FROM Servicio s LEFT JOIN FETCH s.ultimoTecnicoResponsable ORDER BY s.nombre ASC")
    List<Servicio> findAllConTecnico();

    @Query("SELECT s FROM Servicio s LEFT JOIN FETCH s.ultimoTecnicoResponsable WHERE s.id = :id")
    Optional<Servicio> findByIdConTecnico(@Param("id") Long id);
}
