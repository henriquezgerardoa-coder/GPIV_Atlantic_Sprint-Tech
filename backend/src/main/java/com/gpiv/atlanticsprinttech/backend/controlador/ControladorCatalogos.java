package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos")
public class ControladorCatalogos {
    @GetMapping("/roles")
    public List<RolUsuario> listarRolesDisponibles() {
        return Arrays.stream(RolUsuario.values()).toList();
    }
}