package com.gpiv.atlanticsprinttech.backend.proyecto.application;

import com.gpiv.atlanticsprinttech.entities.proyecto.DocumentoPDF;
import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.proyecto.HitoObra;
import com.gpiv.atlanticsprinttech.entities.proyecto.ProyectoProductivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicioProyecto {

    Page<ProyectoProductivo> listar(String identificadorIngreso, EstadoProyecto estado, LocalDate desde, LocalDate hasta, Pageable pageable);

    ProyectoProductivo obtenerPorId(String identificadorIngreso, Long id);

    ProyectoProductivo crear(
        String identificadorIngreso,
        String nombre,
        String descripcion,
        LocalDate fechaEstimadaFin,
        BigDecimal montoInversion,
        Long loteId,
        Long responsableSeguimientoId
    );

    ProyectoProductivo iniciarProyecto(String identificadorIngreso, Long id, Long radicacionId);

    ProyectoProductivo actualizarEstado(String identificadorIngreso, Long id, EstadoProyecto nuevoEstado);

    HitoObra agregarHito(String identificadorIngreso, Long proyectoId, String descripcion, LocalDate fechaVencimientoReal);

    HitoObra marcarHitoCumplido(String identificadorIngreso, Long proyectoId, Long hitoId);

    List<HitoObra> listarHitos(String identificadorIngreso, Long proyectoId);

    DocumentoPDF adjuntarDocumento(String identificadorIngreso, Long proyectoId, String nombreArchivo, byte[] contenido);

    List<DocumentoPDF> listarDocumentos(String identificadorIngreso, Long proyectoId);
}