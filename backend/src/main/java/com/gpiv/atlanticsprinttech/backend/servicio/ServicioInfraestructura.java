package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Servicio;
import com.gpiv.atlanticsprinttech.entities.dominio.ServicioEvento;
import java.util.List;

public interface ServicioInfraestructura {

    List<Servicio> listar();

    Servicio obtener(Long id);

    Servicio crear(String identificadorIngreso, String nombre, String descripcionTecnica);

    Servicio actualizarEstado(String identificadorIngreso, Long servicioId, String estado, String comentario);

    List<ServicioEvento> listarHistorial(Long servicioId);
}
