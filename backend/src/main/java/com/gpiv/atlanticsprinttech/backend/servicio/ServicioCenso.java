package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Censo;
import com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado;
import com.gpiv.atlanticsprinttech.entities.dominio.Vehiculo;
import java.time.LocalDate;
import java.util.List;

public interface ServicioCenso {

    List<Censo> listarCensos(String identificadorIngreso, Long empresaId);

    Censo declararCenso(
        String identificadorIngreso,
        Long empresaId,
        int anioPeriodo,
        int cantidadPersonalRegistrado,
        int cantidadPersonalNoRegistrado,
        String observacion
    );

    List<PersonalRegistrado> listarPersonal(String identificadorIngreso, Long empresaId);

    PersonalRegistrado agregarPersonal(
        String identificadorIngreso,
        Long empresaId,
        String cuit,
        String nombreCompleto,
        LocalDate fechaIngreso
    );

    void eliminarPersonal(String identificadorIngreso, Long empresaId, Long personalId);

    List<Vehiculo> listarVehiculos(String identificadorIngreso, Long empresaId);

    Vehiculo agregarVehiculo(
        String identificadorIngreso,
        Long empresaId,
        String patente,
        String marcaModelo,
        String tipo
    );

    void eliminarVehiculo(String identificadorIngreso, Long empresaId, Long vehiculoId);
}
