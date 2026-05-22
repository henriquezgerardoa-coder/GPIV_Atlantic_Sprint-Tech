package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioSolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaRubro;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Rubro;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos")
public class ControladorCatalogos {

    private final ServicioSolicitudCambioRubro servicioRubro;

    public ControladorCatalogos(ServicioSolicitudCambioRubro servicioRubro) {
        this.servicioRubro = servicioRubro;
    }

    @GetMapping("/roles")
    public List<RolUsuario> listarRolesDisponibles() {
        return Arrays.stream(RolUsuario.values()).toList();
    }

    @GetMapping("/rubros")
    public List<RespuestaRubro> listarRubros() {
        return servicioRubro.listarRubros().stream()
            .map(r -> new RespuestaRubro(r.getId(), r.getNombre(), r.getDescripcion(), r.isRequierePermisoEspecial()))
            .toList();
    }
}