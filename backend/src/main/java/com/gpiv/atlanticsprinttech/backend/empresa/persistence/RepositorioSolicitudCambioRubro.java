package com.gpiv.atlanticsprinttech.backend.empresa.persistence;

import com.gpiv.atlanticsprinttech.entities.empresa.SolicitudCambioRubro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioSolicitudCambioRubro extends JpaRepository<SolicitudCambioRubro, Long> {
}
