package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Rubro;
import com.gpiv.atlanticsprinttech.entities.dominio.SolicitudCambioRubro;
import java.util.List;

public interface ServicioSolicitudCambioRubro {

    List<Rubro> listarRubros();

    List<SolicitudCambioRubro> listar(String identificadorIngreso);

    SolicitudCambioRubro crear(String identificadorIngreso, Long rubroSolicitadoId, String justificacion, String descripcionOtros);

    SolicitudCambioRubro resolver(String identificadorIngreso, Long solicitudId, boolean aprobada, String motivoRechazo, String nombreNuevoRubro);
}
