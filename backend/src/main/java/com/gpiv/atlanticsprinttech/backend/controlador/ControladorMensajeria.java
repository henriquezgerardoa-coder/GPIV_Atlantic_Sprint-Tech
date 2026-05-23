package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMensajeriaDestinatario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudNuevaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
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
    private final MapeadorMensajeria mapeador;

    public ControladorMensajeria(ServicioMensajeria servicioMensajeria, MapeadorMensajeria mapeador) {
        this.servicioMensajeria = servicioMensajeria;
        this.mapeador = mapeador;
    }

    @GetMapping("/destinatarios")
    public List<RespuestaMensajeriaDestinatario> listarDestinatarios(Authentication autenticacion) {
        return servicioMensajeria.listarDestinatarios(autenticacion.getName()).stream()
            .map(mapeador::toDestinatarioRespuesta)
            .toList();
    }

    @GetMapping("/conversaciones")
    public List<RespuestaConversacionMensajeria> listar(Authentication autenticacion) {
        return servicioMensajeria.listarConConMensajes(autenticacion.getName()).stream()
            .map(par -> mapeador.toRespuesta(par.conversacion(), par.mensajes()))
            .toList();
    }

    @GetMapping("/conversaciones/{id}")
    public RespuestaConversacionMensajeria obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        return mapeador.toRespuesta(
            servicioMensajeria.obtenerPorId(autenticacion.getName(), id),
            servicioMensajeria.listarMensajes(autenticacion.getName(), id)
        );
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
        return ResponseEntity
            .created(URI.create("/api/mensajeria/conversaciones/" + conversacion.getId()))
            .body(mapeador.toRespuesta(
                conversacion,
                servicioMensajeria.listarMensajes(autenticacion.getName(), conversacion.getId())
            ));
    }

    @PostMapping("/conversaciones/{id}/mensajes")
    public RespuestaConversacionMensajeria responder(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudRespuestaConversacionMensajeria solicitud,
        Authentication autenticacion
    ) {
        ConversacionMensajeria conversacion = servicioMensajeria.responder(
            autenticacion.getName(), id, solicitud.mensaje()
        );
        return mapeador.toRespuesta(
            conversacion,
            servicioMensajeria.listarMensajes(autenticacion.getName(), id)
        );
    }
}
