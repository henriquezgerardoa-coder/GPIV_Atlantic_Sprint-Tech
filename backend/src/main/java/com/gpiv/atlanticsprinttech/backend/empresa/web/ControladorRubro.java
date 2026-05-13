package com.gpiv.atlanticsprinttech.backend.empresa.web;

import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioRubro;
import com.gpiv.atlanticsprinttech.entities.empresa.Rubro;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rubros")
public class ControladorRubro {
    private final RepositorioRubro repositorioRubro;

    public ControladorRubro(RepositorioRubro repositorioRubro) {
        this.repositorioRubro = repositorioRubro;
    }

    @GetMapping
    public List<Rubro> listar() {
        return repositorioRubro.findAll();
    }
}
