package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioRadicacionHistorial extends JpaRepository<RadicacionHistorial, Long> {

    List<RadicacionHistorial> findByRadicacionIdOrderByFechaEventoDesc(Long radicacionId);
}
