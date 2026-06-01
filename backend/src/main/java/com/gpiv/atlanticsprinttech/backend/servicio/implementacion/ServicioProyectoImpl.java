package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioHitoObra;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import java.time.format.DateTimeFormatter;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioProyecto;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoProyecto;
import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioProyectoImpl implements ServicioProyecto {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RepositorioProyectoProductivo repositorioProyecto;
    private final RepositorioHitoObra repositorioHito;
    private final RepositorioRadicacionSolicitud repositorioRadicacion;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioContextoUsuario servicioContextoUsuario;
    private final ServicioMensajeria servicioMensajeria;

    public ServicioProyectoImpl(
        RepositorioProyectoProductivo repositorioProyecto,
        RepositorioHitoObra repositorioHito,
        RepositorioRadicacionSolicitud repositorioRadicacion,
        RepositorioUsuario repositorioUsuario,
        ServicioContextoUsuario servicioContextoUsuario,
        ServicioMensajeria servicioMensajeria
    ) {
        this.repositorioProyecto = repositorioProyecto;
        this.repositorioHito = repositorioHito;
        this.repositorioRadicacion = repositorioRadicacion;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioContextoUsuario = servicioContextoUsuario;
        this.servicioMensajeria = servicioMensajeria;
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
        if (!servicioContextoUsuario.esRolAdministrador(usuario) && !usuario.tieneRol(RolUsuario.SECRETARIO)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o SECRETARIO pueden crear proyectos");
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

        ProyectoProductivo guardado = repositorioProyecto.save(proyecto);
        notificarNuevoProyecto(identificadorIngreso, guardado);
        return guardado;
    }

    @Override
    public ProyectoProductivo actualizarEstado(String identificadorIngreso, Long proyectoId, String estado) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!servicioContextoUsuario.esRolAdministrador(usuario)
                && !usuario.tieneRol(RolUsuario.SECRETARIO)
                && !servicioContextoUsuario.esRolTecnico(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, SECRETARIO o TECNICO pueden modificar el estado del proyecto");
        }

        ProyectoProductivo proyecto = repositorioProyecto.findByIdConDetalle(proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        EstadoProyecto nuevoEstado;
        try {
            nuevoEstado = EstadoProyecto.valueOf(estado);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + estado);
        }

        proyecto.actualizarEstado(nuevoEstado);
        ProyectoProductivo guardado = repositorioProyecto.save(proyecto);
        if (nuevoEstado == EstadoProyecto.COMPLETADO) {
            notificarProyectoCompletado(identificadorIngreso, guardado);
        }
        return guardado;
    }

    private void notificarNuevoProyecto(String remitenteIngreso, ProyectoProductivo proyecto) {
        String asunto = "[Nuevo] Proyecto productivo: «" + proyecto.getNombre() + "»";
        String cuerpo = "Se ha iniciado el proyecto productivo «" + proyecto.getNombre() + "».";
        List<Usuario> destinatarios = repositorioUsuario.findActivosConRolesGestion(Set.of(RolUsuario.TECNICO));
        for (Usuario dest : destinatarios) {
            if (!dest.getNombreUsuario().equals(remitenteIngreso)) {
                try {
                    servicioMensajeria.crear(remitenteIngreso, dest.getId(), asunto, cuerpo);
                } catch (Exception e) {
                    // Notificación no crítica
                }
            }
        }
    }

    private void notificarProyectoCompletado(String remitenteIngreso, ProyectoProductivo proyecto) {
        String asunto = "[Finalizado] Proyecto: «" + proyecto.getNombre() + "»";
        String cuerpo = "El proyecto productivo «" + proyecto.getNombre()
            + "» ha sido marcado como COMPLETADO.";
        List<Usuario> destinatarios = repositorioUsuario.findActivosConRolesGestion(
            Set.of(RolUsuario.SECRETARIO, RolUsuario.ADMINISTRADOR, RolUsuario.DIRECTIVO));
        for (Usuario dest : destinatarios) {
            if (!dest.getNombreUsuario().equals(remitenteIngreso)) {
                try {
                    servicioMensajeria.crear(remitenteIngreso, dest.getId(), asunto, cuerpo);
                } catch (Exception e) {
                    // Notificación no crítica
                }
            }
        }
    }

    @Override
    public HitoObra agregarHito(
        String identificadorIngreso,
        Long proyectoId,
        String descripcion,
        LocalDate fechaVencimiento
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!servicioContextoUsuario.esRolTecnico(usuario)
                && !servicioContextoUsuario.esRolAdministrador(usuario)
                && !usuario.tieneRol(RolUsuario.SECRETARIO)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, SECRETARIO o TECNICO pueden gestionar hitos");
        }

        ProyectoProductivo proyecto = repositorioProyecto.findById(proyectoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        proyecto.validarPermiteModificarHitos();

        HitoObra hito = HitoObra.crear(proyecto, descripcion, fechaVencimiento);
        HitoObra guardado = repositorioHito.save(hito);
        notificarEmpresaHitosActualizados(proyecto);
        return guardado;
    }

    @Override
    public HitoObra marcarHitoCumplido(String identificadorIngreso, Long proyectoId, Long hitoId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!servicioContextoUsuario.esRolTecnico(usuario)
                && !servicioContextoUsuario.esRolAdministrador(usuario)
                && !usuario.tieneRol(RolUsuario.SECRETARIO)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, SECRETARIO o TECNICO pueden marcar hitos como cumplidos");
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
        if (!servicioContextoUsuario.esRolTecnico(usuario)
                && !servicioContextoUsuario.esRolAdministrador(usuario)
                && !usuario.tieneRol(RolUsuario.SECRETARIO)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, SECRETARIO o TECNICO pueden eliminar hitos");
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

    private void notificarEmpresaHitosActualizados(ProyectoProductivo proyecto) {
        if (proyecto.getSolicitudOrigen() == null) return;
        Long empresaId = proyecto.getSolicitudOrigen().getEmpresa().getId();
        String fechaFin = proyecto.getFechaEstimadaFin() != null
            ? proyecto.getFechaEstimadaFin().format(FMT_FECHA) : "a confirmar";
        List<HitoObra> hitos = repositorioHito.findByProyectoIdOrderByFechaVencimientoRealAsc(proyecto.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("Se ha actualizado el plan de hitos para el proyecto productivo «")
          .append(proyecto.getNombre()).append("».\n\n")
          .append("⏰ FECHA DE VENCIMIENTO DEL PROYECTO: ").append(fechaFin).append("\n\n")
          .append("📋 HITOS DE OBRA:\n");
        for (int i = 0; i < hitos.size(); i++) {
            HitoObra h = hitos.get(i);
            String fechaHito = h.getFechaVencimientoReal() != null
                ? h.getFechaVencimientoReal().format(FMT_FECHA) : "sin fecha";
            sb.append(i + 1).append(". ").append(h.getDescripcion())
              .append(" — Fecha límite: ").append(fechaHito);
            if (h.isCumplido()) sb.append(" ✓");
            sb.append("\n");
        }
        String asunto = "📋 Hitos actualizados — Proyecto «" + proyecto.getNombre() + "»";
        try {
            servicioMensajeria.notificarEmpresa(empresaId, asunto, sb.toString());
        } catch (Exception ignored) {
            // Notificación no crítica
        }
    }
}
