package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMensajeriaDestinatario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMensajeMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MapeadorMensajeria {

    public RespuestaConversacionMensajeria toRespuesta(
        ConversacionMensajeria conversacion,
        List<MensajeMensajeria> mensajes,
        String bandeja
    ) {
        List<RespuestaMensajeMensajeria> mensajesDto = mensajes.stream()
            .map(this::toMensajeRespuesta)
            .toList();
        String ultimoMensaje = mensajesDto.stream()
            .reduce((a, b) -> b)
            .map(RespuestaMensajeMensajeria::contenido)
            .orElse(null);
        return new RespuestaConversacionMensajeria(
            conversacion.getId(),
            conversacion.getTipoOrigen(),
            conversacion.esConsultaPublica(),
            conversacion.getEmpresaId(),
            conversacion.getNombreEmpresa(),
            conversacion.getContactoNombreEmpresa(),
            conversacion.getContactoCorreoElectronico(),
            conversacion.getContactoTelefono(),
            conversacion.getUsuarioResponsableId(),
            conversacion.getUsuarioResponsableNombreUsuario(),
            conversacion.getUsuarioResponsableNombreCompleto(),
            conversacion.getUsuarioIniciadorId(),
            conversacion.getUsuarioIniciadorNombreUsuario(),
            conversacion.getUsuarioIniciadorNombreCompleto(),
            conversacion.getAsunto(),
            conversacion.getFechaCreacion() != null ? conversacion.getFechaCreacion().toString() : null,
            conversacion.getFechaUltimaActualizacion() != null ? conversacion.getFechaUltimaActualizacion().toString() : null,
            ultimoMensaje,
            mensajesDto.size(),
            mensajesDto,
            bandeja
        );
    }

    public RespuestaMensajeMensajeria toMensajeRespuesta(MensajeMensajeria mensaje) {
        return new RespuestaMensajeMensajeria(
            mensaje.getId(),
            mensaje.getEmisorId(),
            mensaje.getEmisorNombreUsuario(),
            mensaje.getEmisorNombreCompleto(),
            mensaje.getEmisorExternoNombre(),
            mensaje.esEmisorExterno(),
            mensaje.getContenido(),
            mensaje.getFechaEnvioTexto()
        );
    }

    public RespuestaMensajeriaDestinatario toDestinatarioRespuesta(Usuario usuario) {
        return new RespuestaMensajeriaDestinatario(
            usuario.getId(),
            usuario.getNombreUsuario(),
            usuario.getNombreCompleto(),
            usuario.getRoles().stream().map(Enum::name).toList()
        );
    }
}