package com.gpiv.atlanticsprinttech.backend.empresa.persistence;

import com.gpiv.atlanticsprinttech.entities.empresa.Rubro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioRubro extends JpaRepository<Rubro, Long> {
}