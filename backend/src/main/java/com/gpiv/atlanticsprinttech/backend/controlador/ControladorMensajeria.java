package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.mapeador.MapeadorMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaBorradorMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaMensajeriaDestinatario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudGuardarBorrador;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudNuevaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRespuestaConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mensajeria")
public class ControladorMensajeria {

    private final ServicioMensajeria servicioMensajeria;
    private final MapeadorMensajeria mapeador;
    private final ServicioContextoUsuario servicioContexto;

    public ControladorMensajeria(ServicioMensajeria servicioMensajeria, MapeadorMensajeria mapeador,
                                  ServicioContextoUsuario servicioContexto) {
        this.servicioMensajeria = servicioMensajeria;
        this.mapeador = mapeador;
        this.servicioContexto = servicioContexto;
    }

    @GetMapping("/destinatarios")
    public List<RespuestaMensajeriaDestinatario> listarDestinatarios(Authentication autenticacion) {
        return servicioMensajeria.listarDestinatarios(autenticacion.getName()).stream()
            .map(mapeador::toDestinatarioRespuesta)
            .toList();
    }

    @GetMapping("/conversaciones")
    public List<RespuestaConversacionMensajeria> listar(Authentication autenticacion) {
        Usuario usuario = servicioContexto.obtenerUsuarioPorIngreso(autenticacion.getName());
        return servicioMensajeria.listarConConMensajes(autenticacion.getName()).stream()
            .map(par -> mapeador.toRespuesta(par.conversacion(), par.mensajes(),
                calcularBandeja(par.conversacion(), usuario)))
            .toList();
    }

    @GetMapping("/conversaciones/{id}")
    public RespuestaConversacionMensajeria obtenerPorId(@PathVariable Long id, Authentication autenticacion) {
        Usuario usuario = servicioContexto.obtenerUsuarioPorIngreso(autenticacion.getName());
        ConversacionMensajeria conv = servicioMensajeria.obtenerPorId(autenticacion.getName(), id);
        return mapeador.toRespuesta(conv,
            servicioMensajeria.listarMensajes(autenticacion.getName(), id),
            calcularBandeja(conv, usuario));
    }

    @PostMapping("/conversaciones")
    public ResponseEntity<RespuestaConversacionMensajeria> crear(
        @Valid @RequestBody SolicitudNuevaConversacionMensajeria solicitud,
        Authentication autenticacion
    ) {
        Usuario usuario = servicioContexto.obtenerUsuarioPorIngreso(autenticacion.getName());
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
                servicioMensajeria.listarMensajes(autenticacion.getName(), conversacion.getId()),
                calcularBandeja(conversacion, usuario)
            ));
    }

    @PostMapping("/conversaciones/{id}/mensajes")
    public RespuestaConversacionMensajeria responder(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudRespuestaConversacionMensajeria solicitud,
        Authentication autenticacion
    ) {
        Usuario usuario = servicioContexto.obtenerUsuarioPorIngreso(autenticacion.getName());
        ConversacionMensajeria conversacion = servicioMensajeria.responder(
            autenticacion.getName(), id, solicitud.mensaje()
        );
        return mapeador.toRespuesta(
            conversacion,
            servicioMensajeria.listarMensajes(autenticacion.getName(), id),
            calcularBandeja(conversacion, usuario)
        );
    }

    // ── Borradores ──────────────────────────────────────────────────────────

    @GetMapping("/borradores")
    public List<RespuestaBorradorMensajeria> listarBorradores(Authentication autenticacion) {
        return servicioMensajeria.listarBorradores(autenticacion.getName());
    }

    @PostMapping("/borradores")
    public ResponseEntity<RespuestaBorradorMensajeria> crearBorrador(
        @Valid @RequestBody SolicitudGuardarBorrador solicitud,
        Authentication autenticacion
    ) {
        RespuestaBorradorMensajeria borrador = servicioMensajeria.guardarBorrador(
            autenticacion.getName(), null, solicitud.destinatarioId(), solicitud.asunto(), solicitud.contenido());
        return ResponseEntity
            .created(URI.create("/api/mensajeria/borradores/" + borrador.id()))
            .body(borrador);
    }

    @PutMapping("/borradores/{id}")
    public RespuestaBorradorMensajeria actualizarBorrador(
        @PathVariable Long id,
        @Valid @RequestBody SolicitudGuardarBorrador solicitud,
        Authentication autenticacion
    ) {
        return servicioMensajeria.guardarBorrador(
            autenticacion.getName(), id, solicitud.destinatarioId(), solicitud.asunto(), solicitud.contenido());
    }

    @DeleteMapping("/borradores/{id}")
    public ResponseEntity<Void> eliminarBorrador(@PathVariable Long id, Authentication autenticacion) {
        servicioMensajeria.eliminarBorrador(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/borradores/{id}/enviar")
    public ResponseEntity<RespuestaConversacionMensajeria> enviarBorrador(
        @PathVariable Long id,
        Authentication autenticacion
    ) {
        Usuario usuario = servicioContexto.obtenerUsuarioPorIngreso(autenticacion.getName());
        ConversacionMensajeria conv = servicioMensajeria.enviarBorrador(autenticacion.getName(), id);
        return ResponseEntity
            .created(URI.create("/api/mensajeria/conversaciones/" + conv.getId()))
            .body(mapeador.toRespuesta(
                conv,
                servicioMensajeria.listarMensajes(autenticacion.getName(), conv.getId()),
                calcularBandeja(conv, usuario)
            ));
    }

    private String calcularBandeja(ConversacionMensajeria conv, Usuario usuario) {
        if (usuario.getId().equals(conv.getUsuarioIniciadorId())) return "SALIDA";
        if (servicioContexto.esRolEmpresa(usuario)) {
            Long eid = usuario.getEmpresaId();
            if (eid != null && eid.equals(conv.getEmpresaId())
                && ConversacionMensajeria.ORIGEN_EMPRESA.equals(conv.getTipoOrigen())) return "SALIDA";
        }
        return "ENTRADA";
    }
}