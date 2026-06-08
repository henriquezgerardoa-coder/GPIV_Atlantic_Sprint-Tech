package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEvaluacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEvaluacionRadicacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEvaluacionEtapa;
import com.gpiv.atlanticsprinttech.entities.dominio.EvaluacionRadicacion;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/radicaciones/{id}/evaluacion")
public class ControladorEvaluacion {

    private final ServicioEvaluacion servicioEvaluacion;

    public ControladorEvaluacion(ServicioEvaluacion servicioEvaluacion) {
        this.servicioEvaluacion = servicioEvaluacion;
    }

    @GetMapping
    public RespuestaEvaluacionRadicacion obtener(
        @PathVariable Long id,
        Authentication autenticacion
    ) {
        return toRespuesta(servicioEvaluacion.obtener(autenticacion.getName(), id));
    }

    @PutMapping("/etapa/{numero}")
    public RespuestaEvaluacionRadicacion guardarEtapa(
        @PathVariable Long id,
        @PathVariable int numero,
        @Valid @RequestBody SolicitudEvaluacionEtapa solicitud,
        Authentication autenticacion
    ) {
        return toRespuesta(servicioEvaluacion.guardarEtapa(autenticacion.getName(), id, numero, solicitud));
    }

    private RespuestaEvaluacionRadicacion toRespuesta(EvaluacionRadicacion e) {
        return new RespuestaEvaluacionRadicacion(
            e.getId(),
            e.getRadicacion().getId(),
            e.getEtapa1EmpleoDirecto(),
            e.getEtapa1MateriaPrimaLocal(),
            e.getEtapa1ImpactoAmbiental(),
            e.getEtapa1Observaciones(),
            e.etapa1Completa(),
            e.puntuacionEtapa1(),
            e.getEtapa2Rentabilidad(),
            e.getEtapa2SolidezFinanciera(),
            e.getEtapa2InversionDeclarada(),
            e.getEtapa2Observaciones(),
            e.etapa2Completa(),
            e.puntuacionEtapa2(),
            e.getEtapa3ViabilidadTecnica(),
            e.getEtapa3CronogramaObra(),
            e.getEtapa3CalidadDocumentacion(),
            e.getEtapa3Observaciones(),
            e.etapa3Completa(),
            e.puntuacionEtapa3(),
            e.puntuacionTotal(),
            e.getEvaluador(),
            e.getFechaActualizacion()
        );
    }
}