package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ServicioProyecto {

    List<ProyectoProductivo> listar(String identificadorIngreso);

    ProyectoProductivo crear(
        String identificadorIngreso,
        String nombre,
        String descripcion,
        BigDecimal montoInversion,
        LocalDate fechaEstimadaFin,
        Long radicacionId,
        Long responsableId
    );

    ProyectoProductivo actualizarEstado(String identificadorIngreso, Long proyectoId, String estado);

    HitoObra agregarHito(String identificadorIngreso, Long proyectoId, String descripcion, LocalDate fechaVencimiento);

    HitoObra marcarHitoCumplido(String identificadorIngreso, Long proyectoId, Long hitoId);

    void eliminarHito(String identificadorIngreso, Long proyectoId, Long hitoId);

    List<HitoObra> listarHitosVencidos();
}
