package com.gpiv.atlanticsprinttech.backend.correo.application;

import com.gpiv.atlanticsprinttech.backend.config.PropiedadesRegistroPublico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ServicioCorreo {

    private static final Logger logger = LoggerFactory.getLogger(ServicioCorreo.class);

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final PropiedadesRegistroPublico propiedades;

    public ServicioCorreo(ObjectProvider<JavaMailSender> javaMailSenderProvider, PropiedadesRegistroPublico propiedades) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.propiedades = propiedades;
    }

    @Async
    public void enviarConfirmacion(String correo, String idTramite) {
        String asunto = "Solicitud de radicación recibida - GPIV";
        String cuerpo = "Hola,\n\n"
            + "Tu solicitud de radicación fue recibida correctamente.\n"
            + "Número de trámite: " + idTramite + "\n\n"
            + "Podés hacer seguimiento del expediente ingresando al sistema con tus credenciales.\n\n"
            + "Soporte: " + propiedades.getContactoSoporte() + "\n"
            + "GPIV - ENREPAVI";
        enviar(correo, asunto, cuerpo);
    }

    @Async
    public void enviarActualizacionEstado(String correo, String idTramite, String nuevoEstado) {
        String asunto = "Actualización de expediente - GPIV";
        String cuerpo = "Hola,\n\n"
            + "Tu expediente " + idTramite + " fue actualizado al estado: " + nuevoEstado + "\n\n"
            + "Ingresá al sistema para más detalles.\n\n"
            + "Soporte: " + propiedades.getContactoSoporte() + "\n"
            + "GPIV - ENREPAVI";
        enviar(correo, asunto, cuerpo);
    }

    private void enviar(String correo, String asunto, String cuerpo) {
        if (!propiedades.getMail().isHabilitado()) {
            logger.info("[MAIL DESHABILITADO] Para={} asunto='{}'", correo, asunto);
            return;
        }
        try {
            JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
            if (sender == null) {
                logger.warn("No hay JavaMailSender configurado, no se puede enviar correo a {}", correo);
                return;
            }
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(propiedades.getMail().getRemitente());
            mensaje.setTo(correo);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            sender.send(mensaje);
            logger.info("Correo enviado a {}", correo);
        } catch (Exception e) {
            logger.error("Error al enviar correo a {}", correo, e);
        }
    }
}
