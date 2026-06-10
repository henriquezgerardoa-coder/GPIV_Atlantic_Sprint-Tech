package com.gpiv.atlanticsprinttech.backend.evento;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ManejadorEventosRadicacion {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ServicioMensajeria servicioMensajeria;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioProyectoProductivo repositorioProyecto;

    public ManejadorEventosRadicacion(
        ServicioMensajeria servicioMensajeria,
        RepositorioUsuario repositorioUsuario,
        RepositorioProyectoProductivo repositorioProyecto
    ) {
        this.servicioMensajeria = servicioMensajeria;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioProyecto = repositorioProyecto;
    }

    @EventListener
    public void onRadicacionCreada(RadicacionCreadaEvent evento) {
        String asunto = "[Nuevo] Radicación: " + evento.radicacion().getNumeroRadicado()
            + " — " + evento.radicacion().getTipoSolicitud();
        String cuerpo = "La empresa «" + evento.radicacion().getEmpresa().getNombre()
            + "» ha presentado una nueva solicitud de radicación ("
            + evento.radicacion().getNumeroRadicado() + ").";
        List<Usuario> destinatarios = repositorioUsuario.findActivosConRolesGestion(
            Set.of(RolUsuario.ADMINISTRADOR, RolUsuario.DIRECTIVO, RolUsuario.SECRETARIO));
        // Usar un usuario de gestión como remitente del sistema para que la conversación
        // no quede vinculada a la empresa solicitante ni aparezca en su bandeja.
        String remitenteSistema = destinatarios.stream()
            .map(Usuario::getNombreUsuario)
            .findFirst()
            .orElse(null);
        if (remitenteSistema == null) return;
        final String rem = remitenteSistema;
        destinatarios.stream()
            .filter(u -> !u.getNombreUsuario().equals(rem))
            .forEach(u -> enviar(rem, u.getId(), asunto, cuerpo));
    }

    @EventListener
    public void onEstadoCambiado(RadicacionEstadoCambiadoEvent evento) {
        String asunto = "Expediente " + evento.radicacion().getNumeroRadicado()
            + " — cambio de estado: " + evento.estadoAnterior().etiquetaLegible()
            + " → " + evento.estadoNuevo().etiquetaLegible();
        String cuerpo = buildCuerpo(evento);

        notificarEmpresa(evento, asunto, cuerpo);

        if (evento.estadoNuevo() == EstadoRadicacion.RADICADA) {
            notificarTecnicos(evento, asunto, cuerpo);
        }

        if (evento.estadoNuevo() == EstadoRadicacion.APROBADA) {
            notificarEmpresaProyectoAprobado(evento);
        }
    }

    private String buildCuerpo(RadicacionEstadoCambiadoEvent evento) {
        String base = "El expediente " + evento.radicacion().getNumeroRadicado()
            + " ha pasado al estado «" + evento.estadoNuevo().etiquetaLegible() + "».";
        return (evento.comentario() != null && !evento.comentario().isBlank())
            ? base + "\n\nObservación: " + evento.comentario()
            : base;
    }

    private void notificarEmpresaProyectoAprobado(RadicacionEstadoCambiadoEvent evento) {
        repositorioProyecto.findBySolicitudOrigenId(evento.radicacion().getId()).ifPresent(proyecto -> {
            String fechaFin = proyecto.getFechaEstimadaFin() != null
                ? proyecto.getFechaEstimadaFin().format(FMT_FECHA) : "a confirmar";
            String asunto = "✅ Proyecto productivo aprobado — " + evento.radicacion().getNumeroRadicado();
            String cuerpo = "Su solicitud de radicación " + evento.radicacion().getNumeroRadicado()
                + " ha sido APROBADA y el proyecto productivo «" + proyecto.getNombre()
                + "» ha sido iniciado.\n\n"
                + "FECHA DE VENCIMIENTO DEL PROYECTO: " + fechaFin + "\n\n"
                + "Los hitos de obra serán comunicados por el equipo técnico del parque industrial.";
            try {
                servicioMensajeria.notificarEmpresa(
                    evento.radicacion().getEmpresa().getId(), asunto, cuerpo);
            } catch (Exception ignored) {
                // Notificación no crítica
            }
        });
    }

    private void notificarEmpresa(RadicacionEstadoCambiadoEvent evento, String asunto, String cuerpo) {
        List<Usuario> usuariosEmpresa = repositorioUsuario
            .findByEmpresaIdConRolesOrderByIdAsc(evento.radicacion().getEmpresa().getId());
        usuariosEmpresa.stream()
            .filter(u -> u.tieneRol(RolUsuario.EMPRESA))
            .filter(u -> !u.getNombreUsuario().equals(evento.identificadorIngreso()))
            .forEach(u -> enviar(evento.identificadorIngreso(), u.getId(), asunto, cuerpo));
    }

    private void notificarTecnicos(RadicacionEstadoCambiadoEvent evento, String asunto, String cuerpo) {
        repositorioUsuario.findActivosConRolesGestion(Set.of(RolUsuario.TECNICO)).stream()
            .filter(u -> !u.getNombreUsuario().equals(evento.identificadorIngreso()))
            .forEach(u -> enviar(evento.identificadorIngreso(), u.getId(), asunto, cuerpo));
    }

    private void enviar(String remitente, Long destinatarioId, String asunto, String cuerpo) {
        try {
            servicioMensajeria.crear(remitente, destinatarioId, asunto, cuerpo);
        } catch (Exception ignored) {
            // Notificación no crítica
        }
    }
}