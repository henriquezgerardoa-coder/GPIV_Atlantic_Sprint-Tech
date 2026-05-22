package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.ServicioEvento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioServicioEvento extends JpaRepository<ServicioEvento, Long> {

    List<ServicioEvento> findByServicioIdOrderByFechaEventoDesc(Long servicioId);
}
