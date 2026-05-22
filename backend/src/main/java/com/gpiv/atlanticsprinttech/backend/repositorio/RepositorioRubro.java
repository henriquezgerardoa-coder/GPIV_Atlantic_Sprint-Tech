package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Rubro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioRubro extends JpaRepository<Rubro, Long> {

    @Query("SELECT r FROM Rubro r ORDER BY r.nombre ASC")
    List<Rubro> findAllOrdenados();
}
