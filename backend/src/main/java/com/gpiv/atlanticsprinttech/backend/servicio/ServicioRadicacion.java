package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRelevamientoPedidoLotes;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.TipoDocumentoRadicacion;
import java.time.LocalDate;
import java.util.List;


public interface ServicioRadicacion {
    List<RadicacionSolicitud> listar(String identificadorIngreso, EstadoRadicacion estado, LocalDate desde, LocalDate hasta);

    RadicacionSolicitud obtenerPorId(String identificadorIngreso, Long id);

    RadicacionSolicitud crear(
        String identificadorIngreso,
        String tipoSolicitud,
        String descripcion,
        String usoEstimativo,
        SolicitudRelevamientoPedidoLotes relevamientoPedidoLotes
    );

    RadicacionSolicitud cambiarEstado(
        String identificadorIngreso,
        Long id,
        EstadoRadicacion estado,
        String comentario,
        Integer tiempoEstimadoObraMeses,
        LocalDate fechaPlazo,
        LocalDate fechaAprobacion,
        String numeroResolucion
    );

    void registrarObservacion(String identificadorIngreso, Long id, String comentario);

    RadicacionDocumento adjuntarDocumento(
        String identificadorIngreso,
        Long id,
        TipoDocumentoRadicacion tipoDocumento,
        String descripcion,
        String nombreArchivo,
        String mimeType,
        byte[] contenido
    );

    List<RadicacionDocumento> listarDocumentos(String identificadorIngreso, Long id);

    RadicacionDocumento obtenerDocumento(String identificadorIngreso, Long radicacionId, Long docId);

    List<RadicacionHistorial> listarHistorial(String identificadorIngreso, Long id);

    RadicacionSolicitud asignarLote(String identificadorIngreso, Long radicacionId, Long loteId);

    RadicacionDocumento subirActaRubrica(
        String identificadorIngreso,
        Long radicacionId,
        String nombreArchivo,
        String mimeType,
        byte[] contenido
    );

    RadicacionDocumento obtenerActaRubrica(String identificadorIngreso, Long radicacionId);
}
