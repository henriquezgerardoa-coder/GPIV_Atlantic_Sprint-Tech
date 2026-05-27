package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEvaluacionEtapa;
import com.gpiv.atlanticsprinttech.entities.dominio.EvaluacionRadicacion;

public interface ServicioEvaluacion {

    EvaluacionRadicacion obtener(String identificadorIngreso, Long radicacionId);

    EvaluacionRadicacion guardarEtapa(String identificadorIngreso, Long radicacionId, int numeroEtapa, SolicitudEvaluacionEtapa solicitud);
}