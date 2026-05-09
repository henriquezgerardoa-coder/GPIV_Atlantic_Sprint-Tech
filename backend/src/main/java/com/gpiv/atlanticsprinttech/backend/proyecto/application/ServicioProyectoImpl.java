package com.gpiv.atlanticsprinttech.backend.proyecto.application;

import com.gpiv.atlanticsprinttech.backend.lote.persistence.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.proyecto.persistence.RepositorioDocumentoPDF;
import com.gpiv.atlanticsprinttech.backend.proyecto.persistence.RepositorioHitoObra;
import com.gpiv.atlanticsprinttech.backend.proyecto.persistence.RepositorioProyecto;
import com.gpiv.atlanticsprinttech.backend.radicacion.persistence.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.usuario.persistence.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.entities.proyecto.DocumentoPDF;
import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.proyecto.HitoObra;
import com.gpiv.atlanticsprinttech.entities.proyecto.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioProyectoImpl implements ServicioProyecto {

    private final RepositorioProyecto repositorioProyecto;
    private final RepositorioHitoObra repositorioHitoObra;
    private final RepositorioDocumentoPDF repositorioDocumentoPDF;
    private final RepositorioLote repositorioLote;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioRadicacionSolicitud repositorioRadicacion;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioProyectoImpl(
        RepositorioProyecto repositorioProyecto,
        RepositorioHitoObra repositorioHitoObra,
        RepositorioDocumentoPDF repositorioDocumentoPDF,
        RepositorioLote repositorioLote,
        RepositorioUsuario repositorioUsuario,
        RepositorioRadicacionSolicitud repositorioRadicacion,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioProyecto = repositorioProyecto;
        this.repositorioHitoObra = repositorioHitoObra;
        this.repositorioDocumentoPDF = repositorioDocumentoPDF;
        this.repositorioLote = repositorioLote;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioRadicacion = repositorioRadicacion;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public Page<ProyectoProductivo> listar(String identificadorIngreso, EstadoProyecto estado, LocalDate desde, LocalDate hasta, Pageable pageable) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaId = null;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            if (usuario.getEmpresaId() == null) return Page.empty(pageable);
            empresaId = usuario.getEmpresaId();
        }
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;
        return repositorioProyecto.buscarFiltrado(empresaId, estado, desdeDateTime, hastaDateTime, pageable);
    }

    @Override
    public ProyectoProductivo obtenerPorId(String identificadorIngreso, Long id) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioProyecto.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        }
        return repositorioProyecto.findByIdConDependencias(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }

    @Override
    public ProyectoProductivo crear(
        String identificadorIngreso,
        String nombre,
        String descripcion,
        LocalDate fechaEstimadaFin,
        BigDecimal montoInversion,
        Long loteId,
        Long responsableSeguimientoId
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMIN o DIRECTIVO pueden crear proyectos");
        }

        Lote lote = repositorioLote.findByIdConEmpresa(loteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
        Empresa empresa = lote.getEmpresa();
        if (empresa == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El lote no tiene empresa asignada");
        }

        Usuario responsable = repositorioUsuario.findById(responsableSeguimientoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsable de seguimiento no encontrado"));

        ProyectoProductivo proyecto = ProyectoProductivo.crear(
            nombre.trim(),
            descripcion.trim(),
            fechaEstimadaFin,
            montoInversion,
            empresa,
            lote,
            responsable
        );
        return repositorioProyecto.save(proyecto);
    }

    @Override
    public ProyectoProductivo iniciarProyecto(String identificadorIngreso, Long id, Long radicacionId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMIN o DIRECTIVO pueden iniciar proyectos");
        }

        ProyectoProductivo proyecto = repositorioProyecto.findByIdConDependencias(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        RadicacionSolicitud radicacion = repositorioRadicacion.findByIdConEmpresa(radicacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicacion no encontrada"));

        try {
            proyecto.iniciarProyecto(radicacion);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return repositorioProyecto.save(proyecto);
    }

    @Override
    public ProyectoProductivo actualizarEstado(String identificadorIngreso, Long id, EstadoProyecto nuevoEstado) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMIN o DIRECTIVO pueden cambiar el estado del proyecto");
        }

        ProyectoProductivo proyecto = repositorioProyecto.findByIdConDependencias(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        try {
            proyecto.actualizarEstado(nuevoEstado);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return repositorioProyecto.save(proyecto);
    }

    @Override
    public HitoObra agregarHito(String identificadorIngreso, Long proyectoId, String descripcion, LocalDate fechaVencimientoReal) {
        ProyectoProductivo proyecto = obtenerPorId(identificadorIngreso, proyectoId);
        HitoObra hito = HitoObra.crear(descripcion.trim(), fechaVencimientoReal, proyecto);
        return repositorioHitoObra.save(hito);
    }

    @Override
    public HitoObra marcarHitoCumplido(String identificadorIngreso, Long proyectoId, Long hitoId) {
        obtenerPorId(identificadorIngreso, proyectoId);
        HitoObra hito = repositorioHitoObra.findByIdAndProyectoId(hitoId, proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hito no encontrado en el proyecto"));
        hito.marcaComoCumplido();
        return repositorioHitoObra.save(hito);
    }

    @Override
    public List<HitoObra> listarHitos(String identificadorIngreso, Long proyectoId) {
        obtenerPorId(identificadorIngreso, proyectoId);
        return repositorioHitoObra.findByProyectoIdOrderByFechaVencimientoReal(proyectoId);
    }

    @Override
    public DocumentoPDF adjuntarDocumento(String identificadorIngreso, Long proyectoId, String nombreArchivo, byte[] contenido) {
        ProyectoProductivo proyecto = obtenerPorId(identificadorIngreso, proyectoId);
        DocumentoPDF doc;
        try {
            doc = DocumentoPDF.crear(nombreArchivo, contenido.length, contenido, proyecto);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return repositorioDocumentoPDF.save(doc);
    }

    @Override
    public List<DocumentoPDF> listarDocumentos(String identificadorIngreso, Long proyectoId) {
        obtenerPorId(identificadorIngreso, proyectoId);
        return repositorioDocumentoPDF.findByProyectoIdOrderByFechaCargaDesc(proyectoId);
    }
}