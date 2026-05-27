package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEvaluacionRadicacion;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioEvaluacion;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEvaluacionEtapa;
import com.gpiv.atlanticsprinttech.entities.dominio.EvaluacionRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioEvaluacionImpl implements ServicioEvaluacion {

    private final RepositorioEvaluacionRadicacion repositorioEvaluacion;
    private final RepositorioRadicacionSolicitud repositorioRadicacion;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioEvaluacionImpl(
        RepositorioEvaluacionRadicacion repositorioEvaluacion,
        RepositorioRadicacionSolicitud repositorioRadicacion,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioEvaluacion = repositorioEvaluacion;
        this.repositorioRadicacion = repositorioRadicacion;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluacionRadicacion obtener(String identificadorIngreso, Long radicacionId) {
        validarAcceso(identificadorIngreso);
        obtenerRadicacionOFallar(radicacionId);
        return repositorioEvaluacion.findByRadicacionId(radicacionId)
            .orElseGet(() -> EvaluacionRadicacion.crear(
                obtenerRadicacionOFallar(radicacionId),
                identificadorIngreso
            ));
    }

    @Override
    public EvaluacionRadicacion guardarEtapa(
        String identificadorIngreso,
        Long radicacionId,
        int numeroEtapa,
        SolicitudEvaluacionEtapa solicitud
    ) {
        validarAcceso(identificadorIngreso);
        RadicacionSolicitud radicacion = obtenerRadicacionOFallar(radicacionId);

        EvaluacionRadicacion evaluacion = repositorioEvaluacion.findByRadicacionId(radicacionId)
            .orElseGet(() -> EvaluacionRadicacion.crear(radicacion, identificadorIngreso));

        switch (numeroEtapa) {
            case 1 -> evaluacion.actualizarEtapa1(
                solicitud.criterio1(), solicitud.criterio2(), solicitud.criterio3(),
                solicitud.observaciones(), identificadorIngreso
            );
            case 2 -> evaluacion.actualizarEtapa2(
                solicitud.criterio1(), solicitud.criterio2(), solicitud.criterio3(),
                solicitud.observaciones(), identificadorIngreso
            );
            case 3 -> evaluacion.actualizarEtapa3(
                solicitud.criterio1(), solicitud.criterio2(), solicitud.criterio3(),
                solicitud.observaciones(), identificadorIngreso
            );
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de etapa inválido: debe ser 1, 2 o 3");
        }

        return repositorioEvaluacion.save(evaluacion);
    }

    private void validarAcceso(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede acceder a la evaluación");
        }
    }

    private RadicacionSolicitud obtenerRadicacionOFallar(Long radicacionId) {
        return repositorioRadicacion.findById(radicacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicación no encontrada"));
    }
}