package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaCensoVehiculo;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaPersonalCenso;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaResumenDeclaracion;
import com.gpiv.atlanticsprinttech.entities.dominio.Censo;
import com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado;
import com.gpiv.atlanticsprinttech.entities.dominio.Vehiculo;
import org.springframework.stereotype.Component;

/**
 * Convierte entidades del dominio Censo a sus DTOs de respuesta.
 * Centraliza todo el mapeo para mantener los controladores limpios.
 */
@Component
public class MapeadorCenso {

    public RespuestaCenso aRespuesta(Censo censo) {
        return new RespuestaCenso(
            censo.getId(),
            censo.getAnioPeriodo(),
            censo.getSemestre(),
            censo.getFechaDeclaracion().toString(),
            censo.getCantidadPersonalRegistrado(),
            censo.getCantidadPersonalNoRegistrado(),
            censo.calcularTotalEmpleados(),
            censo.getObservacion()
        );
    }

    public RespuestaPersonalCenso aRespuestaPersonal(PersonalRegistrado personal) {
        return new RespuestaPersonalCenso(
            personal.getId(),
            personal.getCuit(),
            personal.getNombreCompleto(),
            personal.getFechaIngreso().toString(),
            personal.isDeclarado()
        );
    }

    public RespuestaCensoVehiculo aRespuestaVehiculo(Vehiculo vehiculo) {
        return new RespuestaCensoVehiculo(
            vehiculo.getId(),
            vehiculo.getPatente(),
            vehiculo.getMarcaModelo(),
            vehiculo.getTipo(),
            vehiculo.validarFormatoPatente()
        );
    }
}

