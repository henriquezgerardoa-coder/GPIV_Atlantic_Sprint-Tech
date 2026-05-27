package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioSolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaSolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudResolverCambioRubro;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudSolicitarCambioRubro;
import com.gpiv.atlanticsprinttech.entities.dominio.SolicitudCambioRubro;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cambios-rubro")
public class ControladorSolicitudCambioRubro {

    private final ServicioSolicitudCambioRubro servicio;

    public ControladorSolicitudCambioRubro(ServicioSolicitudCambioRubro servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaSolicitudCambioRubro> listar(Authentication auth) {
        return servicio.listar(auth.getName()).stream()
            .map(this::toRespuesta)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaSolicitudCambioRubro crear(
        @Valid @RequestBody SolicitudSolicitarCambioRubro solicitud,
        Authentication auth
    ) {
        return toRespuesta(servicio.crear(
            auth.getName(),
            solicitud.rubroSolicitadoId(),
            solicitud.justificacion(),
            solicitud.descripcionOtros()
        ));
    }

    @PatchMapping("/{id}/resolver")
    public RespuestaSolicitudCambioRubro resolver(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudResolverCambioRubro solicitud,
        Authentication auth
    ) {
        return toRespuesta(servicio.resolver(
            auth.getName(),
            id,
            solicitud.aprobada(),
            solicitud.motivoRechazo(),
            solicitud.nombreNuevoRubro(),
            solicitud.puntajeImpactoOperativo(),
            solicitud.puntajeCompatibilidadParque(),
            solicitud.puntajeViabilidadTecnica(),
            solicitud.puntajeCumplimientoNormativo(),
            solicitud.observacionesRubrica()
        ));
    }

    private RespuestaSolicitudCambioRubro toRespuesta(SolicitudCambioRubro s) {
        return new RespuestaSolicitudCambioRubro(
            s.getId(),
            s.getEmpresa().getId(),
            s.getEmpresa().getNombre(),
            s.getRubroSolicitado().getId(),
            s.getRubroSolicitado().getNombre(),
            s.getRubroAnteriorNombre(),
            s.getJustificacion(),
            s.getDescripcionOtros(),
            s.getEstado().name(),
            s.getMotivoRechazo(),
            s.getPuntajeImpactoOperativo(),
            s.getPuntajeCompatibilidadParque(),
            s.getPuntajeViabilidadTecnica(),
            s.getPuntajeCumplimientoNormativo(),
            s.getObservacionesRubrica(),
            s.getSolicitadoPor(),
            s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null,
            s.getResueltoPor(),
            s.getFechaResolucion() != null ? s.getFechaResolucion().toString() : null
        );
    }
}
