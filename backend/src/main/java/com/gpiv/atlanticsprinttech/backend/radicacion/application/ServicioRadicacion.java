package com.gpiv.atlanticsprinttech.backend.radicacion.application;

import com.gpiv.atlanticsprinttech.commons.radicacion.dto.SolicitudRelevamientoPedidoLotes;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.radicacion.TipoDocumentoRadicacion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicioRadicacion {
    Page<RadicacionSolicitud> listar(String identificadorIngreso, EstadoRadicacion estado, LocalDate desde, LocalDate hasta, Pageable pageable);

    RadicacionSolicitud obtenerPorId(String identificadorIngreso, Long id);

    RadicacionSolicitud crear(
        String identificadorIngreso,
        String tipoSolicitud,
        String descripcion,
        String usoEstimativo,
        SolicitudRelevamientoPedidoLotes relevamientoPedidoLotes
    );

    RadicacionSolicitud cambiarEstado(String identificadorIngreso, Long id, EstadoRadicacion estado, String comentario);

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

    List<RadicacionHistorial> listarHistorial(String identificadorIngreso, Long id);

    void asignarLote(String identificadorIngreso, Long radicacionId, Long loteId);

    Optional<Lote> obtenerLoteAsignado(String identificadorIngreso, Long radicacionId);
}
