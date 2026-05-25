package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioHitoObra;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioProyecto;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioProyectoImpl implements ServicioProyecto {

    private final RepositorioProyectoProductivo repositorioProyecto;
    private final RepositorioHitoObra repositorioHito;
    private final RepositorioRadicacionSolicitud repositorioRadicacion;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioProyectoImpl(
        RepositorioProyectoProductivo repositorioProyecto,
        RepositorioHitoObra repositorioHito,
        RepositorioRadicacionSolicitud repositorioRadicacion,
        RepositorioUsuario repositorioUsuario,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioProyecto = repositorioProyecto;
        this.repositorioHito = repositorioHito;
        this.repositorioRadicacion = repositorioRadicacion;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoProductivo> listar(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario) && !servicioContextoUsuario.esRolAdministrador(usuario)
                && !servicioContextoUsuario.esRolDirectivo(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioProyecto.findByEmpresaIdConDetalle(empresaId);
        }
        return repositorioProyecto.findAllConDetalle();
    }

    @Override
    public ProyectoProductivo crear(
        String identificadorIngreso,
        String nombre,
        String descripcion,
        BigDecimal montoInversion,
        LocalDate fechaEstimadaFin,
        Long radicacionId,
        Long responsableId
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o DIRECTIVO pueden crear proyectos");
        }

        Usuario responsable = null;
        if (responsableId != null) {
            responsable = repositorioUsuario.findById(responsableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsable no encontrado"));
        }

        ProyectoProductivo proyecto = ProyectoProductivo.crear(nombre, descripcion, montoInversion, fechaEstimadaFin, responsable);

        if (radicacionId != null) {
            RadicacionSolicitud radicacion = repositorioRadicacion.findById(radicacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicacion no encontrada"));
            proyecto.vincularRadicacion(radicacion);
        }

        return repositorioProyecto.save(proyecto);
    }

    @Override
    public ProyectoProductivo actualizarEstado(String identificadorIngreso, Long proyectoId, String estado) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o DIRECTIVO pueden modificar proyectos");
        }

        ProyectoProductivo proyecto = repositorioProyecto.findById(proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        EstadoProyecto nuevoEstado;
        try {
            nuevoEstado = EstadoProyecto.valueOf(estado);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + estado);
        }

        proyecto.actualizarEstado(nuevoEstado);
        return repositorioProyecto.save(proyecto);
    }

    @Override
    public HitoObra agregarHito(
        String identificadorIngreso,
        Long proyectoId,
        String descripcion,
        LocalDate fechaVencimiento
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o DIRECTIVO pueden gestionar hitos");
        }

        ProyectoProductivo proyecto = repositorioProyecto.findById(proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        proyecto.validarPermiteModificarHitos();

        HitoObra hito = HitoObra.crear(proyecto, descripcion, fechaVencimiento);
        return repositorioHito.save(hito);
    }

    @Override
    public HitoObra marcarHitoCumplido(String identificadorIngreso, Long proyectoId, Long hitoId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o DIRECTIVO pueden marcar hitos como cumplidos");
        }

        HitoObra hito = repositorioHito.findByIdAndProyectoId(hitoId, proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hito no encontrado"));

        hito.validarNoCumplido();
        hito.marcarComoCumplido();
        return repositorioHito.save(hito);
    }

    @Override
    public void eliminarHito(String identificadorIngreso, Long proyectoId, Long hitoId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o DIRECTIVO pueden eliminar hitos");
        }

        HitoObra hito = repositorioHito.findByIdAndProyectoId(hitoId, proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hito no encontrado"));

        hito.validarNoCumplido();
        repositorioHito.delete(hito);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HitoObra> listarHitosVencidos() {
        return repositorioHito.findHitosVencidos();
    }
}
