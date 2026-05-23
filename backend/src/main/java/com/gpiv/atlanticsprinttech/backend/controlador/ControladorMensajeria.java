package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMensajeriaDestinatario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMensajeMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudNuevaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mensajeria")
public class ControladorMensajeria {

    private final ServicioMensajeria servicioMensajeria;

    public ControladorMensajeria(ServicioMensajeria servicioMensajeria) {
        this.servicioMensajeria = servicioMensajeria;
    }

    @GetMapping("/destinatarios")
    public List<RespuestaMensajeriaDestinatario> listarDestinatarios(Authentication autenticacion) {
        return servicioMensajeria.listarDestinatarios(autenticacion.getName()).stream()
            .map(this::toDestinatario)
            .toList();
    }

    @GetMapping("/conversaciones")
    public List<RespuestaConversacionMensajeria> listar(Authentication autenticacion) {
        return servicioMensajeria.listar(autenticacion.getName()).stream()
            .map(conversacion -> toRespuesta(conversacion, servicioMensajeria.listarMensajes(autenticacion.getName(), conversacion.getId())))
            .toList();
    }

    @GetMapping("/conversaciones/{id}")
    public RespuestaConversacionMensajeria obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        ConversacionMensajeria conversacion = servicioMensajeria.obtenerPorId(autenticacion.getName(), id);
        return toRespuesta(conversacion, servicioMensajeria.listarMensajes(autenticacion.getName(), id));
    }

    @PostMapping("/conversaciones")
    public ResponseEntity<RespuestaConversacionMensajeria> crear(
        @Valid @RequestBody SolicitudNuevaConversacionMensajeria solicitud,
        Authentication autenticacion
    ) {
        ConversacionMensajeria conversacion = servicioMensajeria.crear(
            autenticacion.getName(),
            solicitud.usuarioResponsableId(),
            solicitud.asunto(),
            solicitud.mensaje()
        );
        return ResponseEntity.created(URI.create("/api/mensajeria/conversaciones/" + conversacion.getId()))
            .body(toRespuesta(conversacion, servicioMensajeria.listarMensajes(autenticacion.getName(), conversacion.getId())));
    }

    @PostMapping("/conversaciones/{id}/mensajes")
    public RespuestaConversacionMensajeria responder(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudRespuestaConversacionMensajeria solicitud,
        Authentication autenticacion
    ) {
        ConversacionMensajeria conversacion = servicioMensajeria.responder(autenticacion.getName(), id, solicitud.mensaje());
        return toRespuesta(conversacion, servicioMensajeria.listarMensajes(autenticacion.getName(), id));
    }

    private RespuestaConversacionMensajeria toRespuesta(ConversacionMensajeria conversacion, List<MensajeMensajeria> mensajes) {
        List<RespuestaMensajeMensajeria> mensajesDto = mensajes.stream().map(this::toMensaje).toList();
        String ultimoMensaje = mensajesDto.isEmpty() ? null : mensajesDto.get(mensajesDto.size() - 1).contenido();
        return new RespuestaConversacionMensajeria(
            conversacion.getId(),
            conversacion.getTipoOrigen(),
            conversacion.esConsultaPublica(),
            conversacion.getEmpresa() != null ? conversacion.getEmpresa().getId() : null,
            conversacion.getEmpresa() != null ? conversacion.getEmpresa().getNombre() : null,
            conversacion.getContactoNombreEmpresa(),
            conversacion.getContactoCorreoElectronico(),
            conversacion.getContactoTelefono(),
            conversacion.getUsuarioResponsable().getId(),
            conversacion.getUsuarioResponsable().getNombreUsuario(),
            conversacion.getUsuarioResponsable().getNombreCompleto(),
            conversacion.getAsunto(),
            conversacion.getFechaCreacion() != null ? conversacion.getFechaCreacion().toString() : null,
            conversacion.getFechaUltimaActualizacion() != null ? conversacion.getFechaUltimaActualizacion().toString() : null,
            ultimoMensaje,
            mensajesDto.size(),
            mensajesDto
        );
    }

    private RespuestaMensajeMensajeria toMensaje(MensajeMensajeria mensaje) {
        Usuario emisor = mensaje.getUsuarioEmisor();
        return new RespuestaMensajeMensajeria(
            mensaje.getId(),
            emisor != null ? emisor.getId() : null,
            emisor != null ? emisor.getNombreUsuario() : null,
            emisor != null ? emisor.getNombreCompleto() : null,
            mensaje.getEmisorExternoNombre(),
            mensaje.esEmisorExterno(),
            mensaje.getContenido(),
            mensaje.getFechaEnvio() != null ? mensaje.getFechaEnvio().toString() : null
        );
    }

    private RespuestaMensajeriaDestinatario toDestinatario(Usuario usuario) {
        return new RespuestaMensajeriaDestinatario(
            usuario.getId(),
            usuario.getNombreUsuario(),
            usuario.getNombreCompleto(),
            usuario.getRoles().stream().map(Enum::name).toList()
        );
    }
}

