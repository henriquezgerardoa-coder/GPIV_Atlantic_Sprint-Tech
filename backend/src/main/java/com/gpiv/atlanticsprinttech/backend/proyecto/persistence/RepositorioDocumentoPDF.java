package com.gpiv.atlanticsprinttech.backend.proyecto.persistence;

import com.gpiv.atlanticsprinttech.entities.proyecto.DocumentoPDF;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDocumentoPDF extends JpaRepository<DocumentoPDF, Long> {

    List<DocumentoPDF> findByProyectoIdOrderByFechaCargaDesc(Long proyectoId);
}