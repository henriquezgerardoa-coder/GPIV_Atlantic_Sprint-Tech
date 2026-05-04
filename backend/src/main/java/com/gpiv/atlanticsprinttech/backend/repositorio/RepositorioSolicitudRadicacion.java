package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.SolicitudRadicacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioSolicitudRadicacion extends JpaRepository<SolicitudRadicacion, Long> {
}